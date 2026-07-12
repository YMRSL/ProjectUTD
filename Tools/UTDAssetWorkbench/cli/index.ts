#!/usr/bin/env node
import { createHash } from "node:crypto";
import { mkdir, readFile, rename, rm, writeFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { buildWorkbenchProject } from "../src/domain/build";
import {
  assertWorkbenchProject,
  exportBlockTransformsJson,
  exportExcelInterfaceJson,
  exportLangOverlaysJson,
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
import type { SourceFingerprint, WorkbenchProject } from "../src/domain/schema";
import { stableStringify } from "../src/domain/stable";

const [, , command = "help", ...argv] = process.argv;

try {
  if (command === "import") await importSources(argv);
  else if (command === "export") await exportProject(argv);
  else if (command === "validate") await validateProject(argv);
  else if (command === "help" || command === "--help" || command === "-h") printHelp();
  else throw new Error(`Unknown command: ${command}`);
} catch (error) {
  console.error(`\n[UTD Asset Workbench] ${error instanceof Error ? error.message : String(error)}\n`);
  process.exitCode = 1;
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
  await writeArtifactBundle(parsed, outDir);
  printSummary(parsed, outDir);
}

async function validateProject(argv: string[]): Promise<void> {
  const options = parseOptions(argv);
  const projectPath = required(options, "project");
  const parsed: unknown = JSON.parse(await readFile(path.resolve(projectPath), "utf8"));
  assertWorkbenchProject(parsed);
  printSummary(parsed, path.dirname(path.resolve(projectPath)));
  const errors = parsed.issues.filter((issue) => issue.severity === "error");
  if (errors.length) {
    for (const issue of errors.slice(0, 20)) console.error(`ERROR ${issue.code}: ${issue.entityId}`);
    process.exitCode = 2;
  }
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

async function atomicWrite(target: string, content: string): Promise<void> {
  await mkdir(path.dirname(target), { recursive: true });
  const temp = path.join(
    path.dirname(target),
    `.${path.basename(target)}.${process.pid}.${Date.now()}.${Math.random().toString(16).slice(2)}.tmp`
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
    const backup = `${target}.${process.pid}.replace-backup`;
    await rm(backup, { force: true });
    try {
      await rename(target, backup);
    } catch (backupError) {
      if ((backupError as NodeJS.ErrnoException).code !== "ENOENT") throw backupError;
    }
    try {
      await rename(temp, target);
    } finally {
      await rm(backup, { force: true });
      await rm(temp, { force: true });
    }
  }
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
    --out <artifacts-dir> \\
    [--public <public/data/workbench.json>] \\
    [--generated-at <ISO-8601 release timestamp>]

Regenerate exports from a canonical project:
  npm run cli -- export --project <workbench.json> --out <artifacts-dir>

Validate a canonical project:
  npm run cli -- validate --project <workbench.json>
`);
}
