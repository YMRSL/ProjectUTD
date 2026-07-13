#!/usr/bin/env node
import { createHash } from "node:crypto";
import { mkdir, readFile, realpath, rename, rm, writeFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import { buildWorkbenchProject } from "../src/domain/build";
import {
  assertWorkbenchProject,
  exportBlockTransformsJson,
  exportExcelInterfaceJson,
  exportLangOverlaysJson,
  exportItemPropertiesJson,
  exportLootRegistryKjs,
  exportPresentationJson,
  exportProjectJson,
  exportRecipeKjs,
  exportStatusJson,
  exportStatusKjs,
  toLangOverlayEntries
} from "../src/domain/exporters";
import { parseJsonOrAssignment } from "../src/domain/parsers";
import { applyBlockTransformDocument, applyPresentationDocument } from "../src/domain/projectCompat";
import { diffBlockTransformSources, exportBlockTransformDiffJson } from "../src/domain/blockTransformDiff";
import type { SourceFingerprint, WorkbenchProject } from "../src/domain/schema";
import { stableStringify } from "../src/domain/stable";
import { applyItemCategoryDocument } from "../src/domain/categories";
import { mergeSnapshotEvidence } from "../src/domain/snapshotMerge";
import { enrichPropertiesFromRuntime } from "./runtimeProperties";
import { itemPropertyIntegrationFiles } from "../src/domain/itemProperties";
import { cleanIdentityCatalog } from "../src/domain/identityCleanup";
import {
  deployPropertyCandidate,
  loadPropertyCandidate,
  rollbackPropertyDeployment
} from "./propertyDeployment";

if (isDirectExecution()) {
  await runCli(process.argv.slice(2));
}

export async function runCli([command = "help", ...argv]: string[]): Promise<void> {
  try {
    if (command === "import") await importSources(argv);
    else if (command === "export") await exportProject(argv);
    else if (command === "merge-snapshot") await mergeSnapshot(argv);
    else if (command === "enrich-properties") await enrichProperties(argv);
    else if (command === "cleanup-identities") await cleanupIdentities(argv);
    else if (command === "deploy-properties") await deployProperties(argv);
    else if (command === "rollback-properties") await rollbackProperties(argv);
    else if (command === "validate") await validateProject(argv);
    else if (command === "diff-block-transforms") await diffBlockTransforms(argv);
    else if (command === "help" || command === "--help" || command === "-h") printHelp();
    else throw new Error(`Unknown command: ${command}`);
  } catch (error) {
    console.error(`\n[UTD Asset Workbench] ${error instanceof Error ? error.message : String(error)}\n`);
    process.exitCode = 1;
  }
}

async function deployProperties(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const candidatePath = path.resolve(required(options, "candidate"));
  const instancePath = path.resolve(required(options, "instance"));
  const dryRun = booleanOption(options, "dry-run");
  const loaded = await loadPropertyCandidate(candidatePath);
  const result = await deployPropertyCandidate(loaded, instancePath, { dryRun });
  console.log(`Property candidate: ${loaded.source}`);
  console.log(`Candidate sha256: ${loaded.sha256}`);
  console.log(`Property deployment${dryRun ? " preview" : ""}: enabled=${result.plan.counts.enabled} rarity=${result.plan.counts.rarity} blockz=${result.plan.counts.blockz} tacz=${result.plan.counts.tacz} food=${result.plan.counts.food}`);
  console.log(`Files: write=${result.plan.writes.length} delete=${result.plan.deletes.length}`);
  if (result.manifest) {
    console.log(`Deployment id: ${result.manifest.deployment_id}`);
    console.log("Restart Minecraft (or the server) before testing RarityCore, BlockZ and TaCZ changes.");
  }
}

async function rollbackProperties(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const instancePath = path.resolve(required(options, "instance"));
  const dryRun = booleanOption(options, "dry-run");
  const force = booleanOption(options, "force");
  const result = await rollbackPropertyDeployment(instancePath, { dryRun, force });
  console.log(`Property rollback${dryRun ? " preview" : ""}: deployment=${result.deploymentId} files=${result.restored.length}`);
  if (!dryRun) console.log("The previous runtime files were restored. Restart Minecraft (or the server) before testing.");
}

async function cleanupIdentities(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const projectPath = path.resolve(required(options, "project"));
  const decisionsPath = path.resolve(required(options, "decisions"));
  const outPath = path.resolve(required(options, "out"));
  const parsed: unknown = JSON.parse(await readFile(projectPath, "utf8"));
  assertWorkbenchProject(parsed);
  const decisions: unknown = JSON.parse(await readFile(decisionsPath, "utf8"));
  if (!decisions || typeof decisions !== "object" || Array.isArray(decisions)) {
    throw new Error("Retired-item decisions must be a JSON object.");
  }
  const rawIds = (decisions as Record<string, unknown>).retired_registry_ids;
  if (!Array.isArray(rawIds) || rawIds.some((value) => typeof value !== "string")) {
    throw new Error("Retired-item decisions must contain retired_registry_ids:string[].");
  }
  const result = cleanIdentityCatalog(parsed, rawIds);
  await atomicWrite(outPath, exportProjectJson(result.project));
  console.log(`Identity cleanup: retired=${result.summary.retiredRegistryIds}, removed_items=${result.summary.removedItems}, removed_recipes=${result.summary.removedRecipes}`);
  console.log(`Gun identity merge: policies=${result.summary.remappedLogicalPolicies}, aliases=${result.summary.mergedAliasItems}, promoted=${result.summary.promotedVariantItems}`);
  console.log(`Clean project: ${outPath}`);
}

async function enrichProperties(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const projectPath = required(options, "project");
  const instancePath = path.resolve(required(options, "instance"));
  const outPath = path.resolve(required(options, "out"));
  const parsed: unknown = JSON.parse(await readFile(path.resolve(projectPath), "utf8"));
  assertWorkbenchProject(parsed);
  const result = await enrichPropertiesFromRuntime(parsed, instancePath);
  await atomicWrite(outPath, exportProjectJson(result.project));
  console.log(`Runtime properties: rarity=${result.summary.rarity}, blockz=${result.summary.blockz}, tacz=${result.summary.tacz}, food=${result.summary.food}`);
  console.log(`Unresolved TaCZ guns: ${result.summary.unresolvedGuns.length}`);
  console.log(`Web project: ${outPath}`);
}

async function mergeSnapshot(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const projectPath = required(options, "project");
  const snapshotPath = required(options, "snapshot");
  const outPath = path.resolve(required(options, "out"));
  const parsed: unknown = JSON.parse(await readFile(path.resolve(projectPath), "utf8"));
  assertWorkbenchProject(parsed);
  const snapshot = await loadSource(snapshotPath, "json");
  let result = mergeSnapshotEvidence(parsed, snapshot.value);
  if (options.categories) {
    const categories = await loadSource(options.categories, "json");
    result = { ...result, project: applyItemCategoryDocument(result.project, categories.value) };
    result.project.manifest.source.categories = categories.fingerprint;
  }
  await atomicWrite(outPath, exportProjectJson(result.project));
  console.log(`Merged snapshot: matched=${result.matched}, icons=${result.icons}, unmatched=${result.unmatched.length}`);
  console.log(`Web project: ${outPath}`);
}

async function importSources(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const snapshotPath = required(options, "snapshot");
  const recipesPath = required(options, "recipes");
  const outDir = path.resolve(options.out ?? "artifacts");
  const snapshot = await loadSource(snapshotPath, "json");
  const recipes = await loadSource(recipesPath, "recipe");
  const lootRegistry = options["loot-registry"] ? await loadSource(options["loot-registry"], "lootRegistry") : undefined;
  const lootBalance = options["loot-balance"] ? await loadSource(options["loot-balance"], "lootBalance") : undefined;
  const blockTransforms = options["block-transforms"] ? await loadSource(options["block-transforms"], "json") : undefined;
  const categories = options.categories ? await loadSource(options.categories, "json") : undefined;
  let project = buildWorkbenchProject({
    snapshot: snapshot.value,
    recipeData: recipes.value,
    lootRegistry: lootRegistry?.value,
    lootBalance: lootBalance?.value,
    projectId: options["project-id"],
    generatedAt: options["generated-at"],
    source: {
      snapshot: snapshot.fingerprint,
      recipes: recipes.fingerprint,
      lootRegistry: lootRegistry?.fingerprint,
      lootBalance: lootBalance?.fingerprint,
      blockTransforms: blockTransforms?.fingerprint
    }
  });
  if (options.presentations) {
    const presentations = await loadSource(options.presentations, "json");
    project = applyPresentationDocument(project, presentations.value);
  }
  if (categories) {
    project = applyItemCategoryDocument(project, categories.value);
    project.manifest.source.categories = categories.fingerprint;
  }
  if (blockTransforms) project = applyBlockTransformDocument(project, blockTransforms.value);
  await writeArtifactBundle(project, outDir);
  if (options.public) {
    const publicPath = path.resolve(options.public);
    await mkdir(path.dirname(publicPath), { recursive: true });
    await atomicWrite(publicPath, exportProjectJson(project));
    console.log(`Public UI data: ${publicPath}`);
  }
  printSummary(project, outDir);
}

async function exportProject(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const projectPath = required(options, "project");
  const outDir = path.resolve(options.out ?? "artifacts");
  const parsed: unknown = JSON.parse(await readFile(path.resolve(projectPath), "utf8"));
  assertWorkbenchProject(parsed);
  let project = parsed;
  if (options.categories) {
    const categories = await loadSource(options.categories, "json");
    project = applyItemCategoryDocument(project, categories.value);
    project.manifest.source.categories = categories.fingerprint;
  }
  await writeArtifactBundle(project, outDir);
  printSummary(project, outDir);
}

async function validateProject(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const projectPath = required(options, "project");
  const only = options.only;
  if (only && only !== "block_transform") {
    throw new Error(`Unsupported validate scope --only ${only}. Expected block_transform.`);
  }
  const parsed: unknown = JSON.parse(await readFile(path.resolve(projectPath), "utf8"));
  assertWorkbenchProject(parsed);
  printSummary(parsed, path.dirname(path.resolve(projectPath)));
  const errors = parsed.issues.filter((issue) =>
    issue.severity === "error" && (!only || issue.entityType === only)
  );
  if (errors.length) {
    for (const issue of errors.slice(0, 20)) console.error(`ERROR ${issue.code}: ${issue.entityId}`);
    process.exitCode = 2;
  }
}

async function diffBlockTransforms(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const baselinePath = path.resolve(required(options, "baseline"));
  const candidatePath = path.resolve(required(options, "candidate"));
  const outPath = options.out ? path.resolve(options.out) : undefined;
  if (outPath) await assertDistinctDiffOutputPaths(baselinePath, candidatePath, outPath);
  const baselineText = await readFile(baselinePath, "utf8");
  const candidateText = await readFile(candidatePath, "utf8");
  const baseline: unknown = JSON.parse(baselineText);
  const candidate: unknown = JSON.parse(candidateText);
  const diff = diffBlockTransformSources(baseline, candidate, {
    baselineSha256: sha256(baselineText),
    candidateSha256: sha256(candidateText)
  });
  const json = exportBlockTransformDiffJson(diff);
  if (outPath) {
    await atomicWrite(outPath, json);
    console.log(`Block transform diff: ${outPath}`);
    console.log(`  baseline  sha256=${diff.summary.baseline_sha256}`);
    console.log(`  candidate sha256=${diff.summary.candidate_sha256}`);
    console.log(
      `  candidate rules=${diff.summary.candidate_rules}`
      + ` enabled=${diff.summary.candidate_enabled_rules}`
    );
    console.log(
      `  +${diff.summary.added} -${diff.summary.removed} ~${diff.summary.modified}`
      + ` enabled=${diff.summary.enabled} disabled=${diff.summary.disabled}`
    );
    return;
  }
  process.stdout.write(json);
}

async function loadSource(filePath: string, kind: "json" | "recipe" | "lootRegistry" | "lootBalance") {
  const absolute = path.resolve(filePath);
  const text = await readFile(absolute, "utf8");
  const markers = {
    json: undefined,
    recipe: "utd.recipeData = ",
    lootRegistry: "utd.lootRegistrySeed = ",
    lootBalance: "utd.generatedLootBalance = "
  } as const;
  const value = parseJsonOrAssignment(text, markers[kind]);
  const fingerprint: SourceFingerprint = {
    path: portableSourcePath(absolute),
    sha256: sha256(text)
  };
  return { value, fingerprint };
}

function portableSourcePath(absolute: string): string {
  const relative = path.relative(process.cwd(), absolute);
  return relative && !relative.startsWith("..") && !path.isAbsolute(relative)
    ? relative.replaceAll("\\", "/")
    : absolute;
}

async function writeArtifactBundle(project: WorkbenchProject, outDir: string): Promise<void> {
  await mkdir(outDir, { recursive: true });
  const files: Record<string, string> = {
    "workbench.json": exportProjectJson(project),
    "utd_recipe_data.generated.js": exportRecipeKjs(project),
    "utd_loot_registry_data.generated.js": exportLootRegistryKjs(project),
    "status_manifest.json": exportStatusJson(project),
    "utd_asset_status_manifest.json": exportStatusJson(project),
    "utd_asset_status_manifest.js": exportStatusKjs(project),
    "utd_asset_excel_interface.json": exportExcelInterfaceJson(project),
    "utd_item_presentations.json": exportPresentationJson(project),
    "utd_lang_overlays.json": exportLangOverlaysJson(project),
    "utd_block_transforms.json": exportBlockTransformsJson(project),
    "utd_item_properties.json": exportItemPropertiesJson(project),
    ...Object.fromEntries(itemPropertyIntegrationFiles(project).map((file) => [file.filename, file.content])),
    ...Object.fromEntries(Object.entries(toLangOverlayEntries(project)).map(([namespace, entries]) => [
      `lang_overlays/${safeNamespace(namespace)}/zh_cn.json`,
      stableStringify(entries, 2) + "\n"
    ]))
  };
  for (const [filename, content] of Object.entries(files)) {
    await atomicWrite(path.join(outDir, filename), content);
  }
  const manifest = {
    schema_version: "utd-artifact-manifest/v1",
    project_id: project.manifest.projectId,
    generated_at: project.manifest.generatedAt,
    catalog_hash: project.manifest.catalogHash,
    deployed_hash: project.manifest.deployedHash,
    source: project.manifest.source,
    counts: project.manifest.counts,
    graph_scope: {
      human_selected_roots: project.graph.rootItemKeys.length,
      visible_nodes: project.graph.nodes.length,
      visible_edges: project.graph.edges.length,
      tag_policy: "reference_only_no_expansion"
    },
    artifacts: Object.fromEntries(
      Object.entries(files).map(([filename, content]) => [filename, {
        sha256: sha256(content),
        bytes: Buffer.byteLength(content, "utf8")
      }])
    )
  };
  await atomicWrite(path.join(outDir, "manifest.json"), stableStringify(manifest, 2) + "\n");
}

export async function atomicWrite(target: string, content: string): Promise<void> {
  await mkdir(path.dirname(target), { recursive: true });
  const nonce = `${process.pid}.${Date.now()}.${Math.random().toString(16).slice(2)}`;
  const temp = path.join(
    path.dirname(target),
    `.${path.basename(target)}.${nonce}.tmp`
  );
  await writeFile(temp, content, "utf8");
  try {
    await rename(temp, target);
  } catch (error) {
    const code = (error as NodeJS.ErrnoException).code;
    if (code !== "EEXIST" && code !== "EPERM") {
      await rm(temp, { force: true });
      throw error;
    }
    const backup = path.join(path.dirname(target), `.${path.basename(target)}.${nonce}.replace-backup`);
    await replaceAfterWindowsRenameConflict(target, temp, backup);
  }
}

export interface AtomicReplaceOperations {
  rename(from: string, to: string): Promise<unknown>;
  rm(target: string, options: { force: true }): Promise<unknown>;
}

/** Completes the Windows replace fallback without ever deleting the only backup on failure. */
export async function replaceAfterWindowsRenameConflict(
  target: string,
  temp: string,
  backup: string,
  operations: AtomicReplaceOperations = { rename, rm }
): Promise<void> {
  let backupCreated = false;
  try {
    await operations.rename(target, backup);
    backupCreated = true;
  } catch (backupError) {
    if ((backupError as NodeJS.ErrnoException).code !== "ENOENT") {
      await operations.rm(temp, { force: true });
      throw backupError;
    }
  }

  try {
    await operations.rename(temp, target);
  } catch (placementError) {
    if (backupCreated) {
      try {
        await operations.rename(backup, target);
        backupCreated = false;
      } catch (restoreError) {
        await operations.rm(temp, { force: true });
        throw new AggregateError(
          [placementError, restoreError],
          `Failed to replace ${target} and restore its backup. Backup retained at ${backup}.`
        );
      }
    }
    await operations.rm(temp, { force: true });
    throw placementError;
  }

  if (backupCreated) await operations.rm(backup, { force: true });
  await operations.rm(temp, { force: true });
}

export async function assertDistinctDiffOutputPaths(
  baseline: string,
  candidate: string,
  out: string,
  platform: NodeJS.Platform = process.platform
): Promise<void> {
  const [canonicalBaseline, canonicalCandidate, canonicalOut] = await Promise.all([
    canonicalPathForComparison(baseline),
    canonicalPathForComparison(candidate),
    canonicalPathForComparison(out)
  ]);
  if (pathsEqualForPlatform(canonicalOut, canonicalBaseline, platform)) {
    throw new Error("--out must not overwrite --baseline.");
  }
  if (pathsEqualForPlatform(canonicalOut, canonicalCandidate, platform)) {
    throw new Error("--out must not overwrite --candidate.");
  }
}

export async function canonicalPathForComparison(value: string): Promise<string> {
  const absolute = path.resolve(value);
  try {
    return path.normalize(await realpath(absolute));
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
  }
  try {
    const parent = await realpath(path.dirname(absolute));
    return path.normalize(path.join(parent, path.basename(absolute)));
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
    return path.normalize(absolute);
  }
}

export function pathsEqualForPlatform(
  left: string,
  right: string,
  platform: NodeJS.Platform = process.platform
): boolean {
  const normalizedLeft = path.normalize(left);
  const normalizedRight = path.normalize(right);
  return platform === "win32"
    ? normalizedLeft.toLowerCase() === normalizedRight.toLowerCase()
    : normalizedLeft === normalizedRight;
}

function isDirectExecution(): boolean {
  const entry = process.argv[1];
  return Boolean(entry) && pathsEqualForPlatform(fileURLToPath(import.meta.url), path.resolve(entry));
}

function printSummary(project: WorkbenchProject, outDir: string): void {
  console.log("\nUTD Asset Workbench");
  console.log(`  catalog        ${project.items.length}`);
  console.log(`  human selected ${project.graph.rootItemKeys.length}`);
  console.log(`  recipes        ${project.recipes.length}`);
  console.log(`  loot rows      ${project.lootPolicies.length}`);
  console.log(`  presentations  ${project.presentations.length}`);
  console.log(`  block rules    ${project.blockTransforms.length}`);
  console.log(`  graph nodes    ${project.graph.nodes.length}`);
  console.log(`  cycles         ${project.graph.cycles.length}`);
  console.log(`  issues         ${project.issues.length}`);
  console.log(`  catalog hash   ${project.manifest.catalogHash}`);
  console.log(`  output         ${outDir}\n`);
}

function safeNamespace(value: string): string {
  return value.toLocaleLowerCase().replace(/[^a-z0-9_.-]+/g, "_") || "unknown";
}

function parseOptions(argv: string[]): Record<string, string> {
  const options: Record<string, string> = {};
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    if (!token.startsWith("--")) throw new Error(`Unexpected argument: ${token}`);
    const [rawKey, inlineValue] = token.slice(2).split("=", 2);
    const value = inlineValue ?? argv[index + 1];
    if (!value || value.startsWith("--")) throw new Error(`Missing value for --${rawKey}`);
    options[rawKey] = value;
    if (inlineValue === undefined) index += 1;
  }
  return options;
}

function required(options: Record<string, string>, key: string): string {
  const value = options[key];
  if (!value) throw new Error(`Missing required option --${key}`);
  return value;
}

function booleanOption(options: Record<string, string>, key: string): boolean {
  const value = options[key];
  if (value === undefined) return false;
  if (value === "true" || value === "1" || value === "yes") return true;
  if (value === "false" || value === "0" || value === "no") return false;
  throw new Error(`--${key} must be true or false.`);
}

function sha256(text: string): string {
  return createHash("sha256").update(text, "utf8").digest("hex");
}

function printHelp(): void {
  console.log(`
UTD Asset Workbench data CLI

Import runtime sources:
  npm run cli -- import \\
    --snapshot <whitelist.snapshot.json> \\
    --recipes <utd_recipe_data.js> \\
    [--loot-registry <utd_loot_registry_data.js>] \\
    [--loot-balance <utd_loot_balance_data.js>] \\
    [--presentations <presentation_drafts.json>] \\
    [--block-transforms <utd_block_transforms.json>] \\
    [--categories <utd_item_categories.json>] \\
    --out <artifacts-dir> \\
    [--public <public/data/workbench.json>] \\
    [--generated-at <ISO-8601 release timestamp>]

Regenerate exports from a canonical project:
  npm run cli -- export --project <workbench.json> [--categories <utd_item_categories.json>] --out <artifacts-dir>

Merge the latest game export icons into a canonical web directory:
  npm run cli -- merge-snapshot --project <workbench.json> --snapshot <utd-assets.json> \\
    [--categories <utd_item_categories.json>] --out <web-workbench.json>

Enrich a web project with current RarityCore, BlockZ, TaCZ and FPE values:
  npm run cli -- enrich-properties --project <workbench.json> --instance <game-instance-root> --out <web-workbench.json>

Deploy an audited browser candidate ZIP (or workbench.json) into one game instance:
  npm run cli -- deploy-properties --candidate <project.candidate.zip|workbench.json> \\
    --instance <game-instance-root> [--dry-run true]

Roll back the latest property deployment from its verified backup:
  npm run cli -- rollback-properties --instance <game-instance-root> [--dry-run true] [--force true]

Apply confirmed retired-item decisions and merge logical TaCZ gun aliases:
  npm run cli -- cleanup-identities --project <workbench.json> \\
    --decisions <utd_retired_items.json> --out <clean-workbench.json>

Validate a canonical project:
  npm run cli -- validate --project <workbench.json> [--only block_transform]

Compare deployed and candidate block transform rules:
  npm run cli -- diff-block-transforms \\
    --baseline <utd_block_transforms.json|workbench.json> \\
    --candidate <utd_block_transforms.json|workbench.json> \\
    [--out <utd_block_transform_diff.json>]
`);
}
