import { createHash } from "node:crypto";
import { copyFile, mkdir, readFile, rename, rm, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import JSZip from "jszip";
import { assertWorkbenchProject, exportItemPropertiesJson } from "../src/domain/exporters";
import {
  itemPropertyIntegrationFiles,
  toBlockZGridDocument
} from "../src/domain/itemProperties";
import type { JsonObject, WorkbenchProject } from "../src/domain/schema";
import { stableStringify } from "../src/domain/stable";

const DEPLOYMENT_SCHEMA = "utd-item-property-deployment/v1";
const BACKUP_SCHEMA = "utd-item-property-backup/v1";
const CANDIDATE_SCHEMA = "utd-browser-candidate-package/v1";
const DEPLOYMENT_MANIFEST = "config/utd_asset_manager/property_deployment.json";
const RARITY_BASE = "config/raritycore/FinalRarityConfig/utd_asset_workbench.json";
const BLOCKZ_CONFIG = "config/blockz/grid_items.json";
const FOOD_CONFIG = "config/firstpersonfoodeating/utd_food_overrides.json";
const PROPERTY_CONFIG = "config/utd_asset_manager/item_properties.json";

export interface LoadedPropertyCandidate {
  project: WorkbenchProject;
  sha256: string;
  source: string;
}

export interface PropertyDeploymentPlan {
  projectId: string;
  catalogHash: string;
  writes: Array<{ path: string; content: string; sha256: string }>;
  deletes: string[];
  counts: { enabled: number; rarity: number; blockz: number; tacz: number; food: number };
  blockzRegistryIds: string[];
  blockzVariantRules: string[];
}

interface DeploymentManifest {
  schema_version: typeof DEPLOYMENT_SCHEMA;
  deployment_id: string;
  project_id: string;
  catalog_hash: string;
  candidate_sha256: string;
  deployed_at: string;
  backup_manifest: string;
  managed_files: Array<{ path: string; sha256: string | null }>;
  blockz_registry_ids: string[];
  blockz_variant_rules: string[];
  counts: PropertyDeploymentPlan["counts"];
  restart_required: boolean;
}

interface BackupManifest {
  schema_version: typeof BACKUP_SCHEMA;
  deployment_id: string;
  created_at: string;
  entries: Array<{
    path: string;
    existed: boolean;
    sha256: string | null;
    backup_path: string | null;
  }>;
}

export async function loadPropertyCandidate(candidatePath: string): Promise<LoadedPropertyCandidate> {
  const absolute = path.resolve(candidatePath);
  const bytes = await readFile(absolute);
  const candidateSha = sha256(bytes);
  if (!absolute.toLowerCase().endsWith(".zip")) {
    const parsed: unknown = JSON.parse(bytes.toString("utf8"));
    assertWorkbenchProject(parsed);
    return { project: parsed, sha256: candidateSha, source: absolute };
  }

  const zip = await JSZip.loadAsync(bytes);
  const manifestEntry = zip.file("manifest.json");
  const projectEntry = zip.file("workbench.json");
  if (!manifestEntry || !projectEntry) throw new Error("Candidate ZIP must contain manifest.json and workbench.json.");
  const manifest: unknown = JSON.parse(await manifestEntry.async("string"));
  if (!isCandidateManifest(manifest)) throw new Error("Candidate ZIP manifest is invalid or unsupported.");
  for (const entry of manifest.files) {
    const archived = zip.file(entry.path);
    if (!archived) throw new Error(`Candidate ZIP is missing ${entry.path}.`);
    const archivedBytes = await archived.async("uint8array");
    if (archivedBytes.byteLength !== entry.bytes || sha256(archivedBytes) !== entry.sha256) {
      throw new Error(`Candidate ZIP integrity check failed for ${entry.path}.`);
    }
  }
  const parsed: unknown = JSON.parse(await projectEntry.async("string"));
  assertWorkbenchProject(parsed);
  if (parsed.manifest.projectId !== manifest.project_id || parsed.manifest.catalogHash !== manifest.catalog_hash) {
    throw new Error("Candidate ZIP project identity does not match its manifest.");
  }
  return { project: parsed, sha256: candidateSha, source: absolute };
}

export async function planPropertyDeployment(
  project: WorkbenchProject,
  instanceRoot: string
): Promise<PropertyDeploymentPlan> {
  exportItemPropertiesJson(project);
  const instance = await assertInstanceRoot(instanceRoot);
  const previous = await readDeploymentManifest(instance);
  const integrations = itemPropertyIntegrationFiles(project);
  const writes = new Map<string, string>();
  writes.set(PROPERTY_CONFIG, exportItemPropertiesJson(project));

  const rarityBase = integrations.find((file) => file.filename.endsWith("/FinalRarityConfig/utd_asset_workbench.json"));
  writes.set(RARITY_BASE, rarityBase?.content ?? "{}\n");

  for (const file of integrations) {
    const rarityMatch = /^integrations\/raritycore\/item_data_matches\/(.+\.json)$/i.exec(file.filename);
    if (rarityMatch) {
      writes.set(`config/raritycore/item_data_matches/utd_asset_workbench/${rarityMatch[1]}`, file.content);
      continue;
    }
    const taczMatch = /^integrations\/tacz\/(.+)$/i.exec(file.filename);
    if (taczMatch) {
      writes.set(`tacz/${taczMatch[1]}`, file.content);
    }
  }

  const enabled = project.itemProperties.filter((entry) => entry.enabled);
  const blockzRows = enabled.filter((entry) => entry.blockz);
  const blockzRegistryIds = blockzRows.filter((entry) => !entry.variantDiscriminator).map((entry) => entry.registryId);
  const blockzVariantRules = blockzRows.filter((entry) => entry.variantDiscriminator)
    .map((entry) => `${entry.registryId}\u0000${entry.variantDiscriminator}`);
  const currentBlockz = await readJsonObject(path.join(instance, BLOCKZ_CONFIG));
  const mergedBlockz = mergeBlockZDocument(
    currentBlockz,
    toBlockZGridDocument(project),
    previous?.blockz_registry_ids ?? [],
    previous?.blockz_variant_rules ?? []
  );
  writes.set(BLOCKZ_CONFIG, stableStringify(mergedBlockz, 2) + "\n");

  const food = integrations.find((file) => file.filename === "integrations/firstpersonfoodeating/utd_food_overrides.json");
  writes.set(FOOD_CONFIG, food?.content ?? stableStringify({
    schema_version: "utd-food-property-overrides/v1",
    foods: []
  }, 2) + "\n");

  if (enabled.some((entry) => entry.tacz)) {
    const metaPath = "tacz/utd_workbench_pack/gunpack.meta.json";
    if (!(await isFile(path.join(instance, metaPath)))) {
      writes.set(metaPath, stableStringify({ namespace: "utd_workbench_pack" }, 2) + "\n");
    }
  }

  const currentPaths = new Set(writes.keys());
  const deletes = (previous?.managed_files ?? [])
    .map((entry) => entry.path)
    .filter((entry) => isOwnedReplaceablePath(entry) && !currentPaths.has(entry))
    .sort((left, right) => left.localeCompare(right, "en"));
  const materializedWrites = [...writes.entries()]
    .map(([relative, content]) => ({ path: relative, content, sha256: sha256(content) }))
    .sort((left, right) => left.path.localeCompare(right.path, "en"));
  return {
    projectId: project.manifest.projectId,
    catalogHash: project.manifest.catalogHash,
    writes: materializedWrites,
    deletes,
    counts: {
      enabled: enabled.length,
      rarity: enabled.filter((entry) => entry.rarity).length,
      blockz: blockzRows.length,
      tacz: enabled.filter((entry) => entry.tacz).length,
      food: enabled.filter((entry) => entry.food).length
    },
    blockzRegistryIds: [...new Set(blockzRegistryIds)].sort((left, right) => left.localeCompare(right, "en")),
    blockzVariantRules: [...new Set(blockzVariantRules)].sort((left, right) => left.localeCompare(right, "en"))
  };
}

export async function deployPropertyCandidate(
  loaded: LoadedPropertyCandidate,
  instanceRoot: string,
  options: { dryRun?: boolean; deployedAt?: string } = {}
): Promise<{ plan: PropertyDeploymentPlan; manifest?: DeploymentManifest }> {
  const instance = await assertInstanceRoot(instanceRoot);
  const plan = await planPropertyDeployment(loaded.project, instance);
  if (options.dryRun) return { plan };

  const deployedAt = options.deployedAt ?? new Date().toISOString();
  const deploymentId = safeTimestamp(deployedAt);
  const backupRoot = `config/utd_asset_manager/property_backups/${deploymentId}`;
  const backupManifestPath = `${backupRoot}/backup_manifest.json`;
  const targets = [...new Set([
    ...plan.writes.map((entry) => entry.path),
    ...plan.deletes,
    DEPLOYMENT_MANIFEST
  ])].sort((left, right) => left.localeCompare(right, "en"));
  const backup = await createBackup(instance, deploymentId, backupRoot, targets, deployedAt);
  await atomicWrite(path.join(instance, backupManifestPath), stableStringify(backup, 2) + "\n");

  const manifest: DeploymentManifest = {
    schema_version: DEPLOYMENT_SCHEMA,
    deployment_id: deploymentId,
    project_id: plan.projectId,
    catalog_hash: plan.catalogHash,
    candidate_sha256: loaded.sha256,
    deployed_at: deployedAt,
    backup_manifest: backupManifestPath,
    managed_files: [
      ...plan.writes.map((entry) => ({ path: entry.path, sha256: entry.sha256 })),
      ...plan.deletes.map((entry) => ({ path: entry, sha256: null }))
    ].sort((left, right) => left.path.localeCompare(right.path, "en")),
    blockz_registry_ids: plan.blockzRegistryIds,
    blockz_variant_rules: plan.blockzVariantRules,
    counts: plan.counts,
    restart_required: true
  };

  try {
    for (const entry of plan.writes) await atomicWrite(path.join(instance, entry.path), entry.content);
    for (const relative of plan.deletes) await rm(path.join(instance, relative), { force: true });
    await atomicWrite(path.join(instance, DEPLOYMENT_MANIFEST), stableStringify(manifest, 2) + "\n");
  } catch (error) {
    await restoreBackup(instance, backup);
    throw new AggregateError([error], `Property deployment failed; the previous runtime state was restored from ${backupManifestPath}.`);
  }
  return { plan, manifest };
}

export async function rollbackPropertyDeployment(
  instanceRoot: string,
  options: { dryRun?: boolean; force?: boolean } = {}
): Promise<{ deploymentId: string; restored: string[] }> {
  const instance = await assertInstanceRoot(instanceRoot);
  const current = await readDeploymentManifest(instance);
  if (!current) throw new Error("No UTD property deployment is available to roll back.");
  const backupPath = safeTarget(instance, current.backup_manifest);
  const parsed: unknown = JSON.parse(await readFile(backupPath, "utf8"));
  if (!isBackupManifest(parsed) || parsed.deployment_id !== current.deployment_id) {
    throw new Error("The property deployment backup manifest is missing or does not match the active deployment.");
  }
  if (!options.force) await assertDeploymentUnchanged(instance, current);
  const restored = parsed.entries.map((entry) => entry.path);
  if (!options.dryRun) await restoreBackup(instance, parsed);
  return { deploymentId: current.deployment_id, restored };
}

function mergeBlockZDocument(
  current: JsonObject,
  candidate: JsonObject,
  previousRegistryIds: string[],
  previousVariantRules: string[]
): JsonObject {
  const root = structuredClone(current);
  const items = objectOr(root.items);
  for (const registryId of previousRegistryIds) delete items[registryId];
  Object.assign(items, objectOr(candidate.items));
  root.items = items;

  const previousVariants = new Set(previousVariantRules);
  const existingNbt = Array.isArray(root.nbt_items) ? root.nbt_items.filter(isJsonObject) : [];
  const retained = existingNbt.filter((entry) => {
    const registryId = text(entry.id);
    const key = text(entry.nbt_key);
    const value = text(entry.nbt_value);
    return !previousVariants.has(`${registryId}\u0000${discriminatorFromNbt(key, value)}`);
  });
  const candidateNbt = Array.isArray(candidate.nbt_items) ? candidate.nbt_items.filter(isJsonObject) : [];
  root.nbt_items = [...retained, ...candidateNbt];
  return root;
}

function discriminatorFromNbt(key: string, value: string): string {
  const normalizedKey = key === "firstpersonfoodeating_profile.food_id" ? "food_id" : key;
  return normalizedKey && value ? `${normalizedKey}=${value}` : "";
}

async function createBackup(
  instance: string,
  deploymentId: string,
  backupRoot: string,
  targets: string[],
  createdAt: string
): Promise<BackupManifest> {
  const entries: BackupManifest["entries"] = [];
  for (let index = 0; index < targets.length; index += 1) {
    const relative = targets[index];
    const target = safeTarget(instance, relative);
    const existed = await isFile(target);
    let backupPath: string | null = null;
    let digest: string | null = null;
    if (existed) {
      const bytes = await readFile(target);
      digest = sha256(bytes);
      backupPath = `${backupRoot}/files/${String(index + 1).padStart(4, "0")}-${safeFilename(path.basename(relative))}`;
      const backupTarget = safeTarget(instance, backupPath);
      await mkdir(path.dirname(backupTarget), { recursive: true });
      await copyFile(target, backupTarget);
    }
    entries.push({ path: relative, existed, sha256: digest, backup_path: backupPath });
  }
  return { schema_version: BACKUP_SCHEMA, deployment_id: deploymentId, created_at: createdAt, entries };
}

async function restoreBackup(instance: string, backup: BackupManifest): Promise<void> {
  for (const entry of backup.entries) {
    const target = safeTarget(instance, entry.path);
    if (!entry.existed) {
      await rm(target, { force: true });
      continue;
    }
    if (!entry.backup_path) throw new Error(`Backup entry has no source for ${entry.path}.`);
    const source = safeTarget(instance, entry.backup_path);
    const bytes = await readFile(source);
    if (entry.sha256 && sha256(bytes) !== entry.sha256) throw new Error(`Backup integrity check failed for ${entry.path}.`);
    await mkdir(path.dirname(target), { recursive: true });
    await copyFile(source, target);
  }
}

async function assertDeploymentUnchanged(instance: string, manifest: DeploymentManifest): Promise<void> {
  const changed: string[] = [];
  for (const entry of manifest.managed_files) {
    const target = safeTarget(instance, entry.path);
    const exists = await isFile(target);
    if (entry.sha256 === null ? exists : !exists || sha256(await readFile(target)) !== entry.sha256) changed.push(entry.path);
  }
  if (changed.length) {
    throw new Error(`Rollback stopped because deployed files changed after release: ${changed.slice(0, 6).join(", ")}. Use --force true only after reviewing those files.`);
  }
}

async function readDeploymentManifest(instance: string): Promise<DeploymentManifest | null> {
  try {
    const parsed: unknown = JSON.parse(await readFile(path.join(instance, DEPLOYMENT_MANIFEST), "utf8"));
    return isDeploymentManifest(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

async function assertInstanceRoot(value: string): Promise<string> {
  const absolute = path.resolve(value);
  const info = await stat(absolute).catch(() => null);
  if (!info?.isDirectory()) throw new Error(`Game instance root does not exist: ${absolute}`);
  if (!(await isFile(path.join(absolute, "options.txt"))) && !(await isDirectory(path.join(absolute, "mods")))) {
    throw new Error(`Refusing to deploy outside a Minecraft instance: ${absolute}`);
  }
  return absolute;
}

function safeTarget(instance: string, relative: string): string {
  if (!relative || path.isAbsolute(relative)) throw new Error(`Unsafe deployment path: ${relative}`);
  const absolute = path.resolve(instance, relative);
  const back = path.relative(instance, absolute);
  if (!back || back.startsWith("..") || path.isAbsolute(back)) throw new Error(`Deployment path escapes the instance: ${relative}`);
  return absolute;
}

async function atomicWrite(target: string, content: string): Promise<void> {
  await mkdir(path.dirname(target), { recursive: true });
  const temporary = path.join(path.dirname(target), `.${path.basename(target)}.${process.pid}.${Date.now()}.tmp`);
  await writeFile(temporary, content, "utf8");
  try {
    await rename(temporary, target);
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== "EEXIST" && (error as NodeJS.ErrnoException).code !== "EPERM") {
      await rm(temporary, { force: true });
      throw error;
    }
    const previous = `${temporary}.previous`;
    let moved = false;
    try {
      await rename(target, previous);
      moved = true;
    } catch (moveError) {
      if ((moveError as NodeJS.ErrnoException).code !== "ENOENT") throw moveError;
    }
    try {
      await rename(temporary, target);
      if (moved) await rm(previous, { force: true });
    } catch (placeError) {
      if (moved) await rename(previous, target);
      await rm(temporary, { force: true });
      throw placeError;
    }
  }
}

async function readJsonObject(file: string): Promise<JsonObject> {
  try {
    const text = await readFile(file, "utf8");
    return objectOr(JSON.parse(stripJsonComments(text).replace(/,\s*([}\]])/g, "$1")));
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === "ENOENT") return {};
    throw new Error(`Cannot safely merge existing JSON file ${file}: ${error instanceof Error ? error.message : String(error)}`);
  }
}

function stripJsonComments(textValue: string): string {
  let result = "";
  let inString = false;
  let escaped = false;
  for (let index = 0; index < textValue.length; index += 1) {
    const current = textValue[index];
    const next = textValue[index + 1];
    if (inString) {
      result += current;
      if (escaped) escaped = false;
      else if (current === "\\") escaped = true;
      else if (current === '"') inString = false;
    } else if (current === '"') {
      inString = true;
      result += current;
    } else if (current === "/" && next === "/") {
      while (index < textValue.length && textValue[index] !== "\n") index += 1;
      result += "\n";
    } else if (current === "/" && next === "*") {
      index += 2;
      while (index < textValue.length - 1 && !(textValue[index] === "*" && textValue[index + 1] === "/")) index += 1;
      index += 1;
    } else result += current;
  }
  return result;
}

function isOwnedReplaceablePath(value: string): boolean {
  return value.startsWith("config/raritycore/item_data_matches/utd_asset_workbench/")
    || /^tacz\/utd_workbench_pack\/data\/[^/]+\/data\/guns\/.+\.json$/i.test(value);
}

function isCandidateManifest(value: unknown): value is {
  schema_version: typeof CANDIDATE_SCHEMA;
  project_id: string;
  catalog_hash: string;
  files: Array<{ path: string; sha256: string; bytes: number }>;
} {
  if (!isJsonObject(value) || value.schema_version !== CANDIDATE_SCHEMA || typeof value.project_id !== "string"
    || typeof value.catalog_hash !== "string" || !Array.isArray(value.files)) return false;
  return value.files.every((entry) => isJsonObject(entry) && typeof entry.path === "string"
    && /^[a-f0-9]{64}$/.test(text(entry.sha256)) && Number.isInteger(entry.bytes) && Number(entry.bytes) >= 0);
}

function isDeploymentManifest(value: unknown): value is DeploymentManifest {
  return isJsonObject(value) && value.schema_version === DEPLOYMENT_SCHEMA
    && typeof value.deployment_id === "string" && typeof value.backup_manifest === "string"
    && Array.isArray(value.managed_files) && Array.isArray(value.blockz_registry_ids)
    && Array.isArray(value.blockz_variant_rules);
}

function isBackupManifest(value: unknown): value is BackupManifest {
  return isJsonObject(value) && value.schema_version === BACKUP_SCHEMA && typeof value.deployment_id === "string"
    && Array.isArray(value.entries) && value.entries.every((entry) => isJsonObject(entry)
      && typeof entry.path === "string" && typeof entry.existed === "boolean");
}

function isJsonObject(value: unknown): value is JsonObject {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function objectOr(value: unknown): JsonObject {
  return isJsonObject(value) ? value : {};
}

function text(value: unknown): string {
  return typeof value === "string" ? value : "";
}

async function isFile(value: string): Promise<boolean> {
  return (await stat(value).catch(() => null))?.isFile() ?? false;
}

async function isDirectory(value: string): Promise<boolean> {
  return (await stat(value).catch(() => null))?.isDirectory() ?? false;
}

function safeTimestamp(value: string): string {
  return value.replace(/[^0-9A-Za-z_-]+/g, "-").replace(/^-+|-+$/g, "") || String(Date.now());
}

function safeFilename(value: string): string {
  return value.replace(/[^0-9A-Za-z_.-]+/g, "_") || "file";
}

function sha256(value: string | Uint8Array): string {
  return createHash("sha256").update(value).digest("hex");
}
