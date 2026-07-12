import JSZip from "jszip";
import { exportBlockTransformsJson, exportPresentationJson, exportProjectJson } from "./exporters";
import type { WorkbenchProject } from "./schema";
import { stableStringify } from "./stable";

export const CANDIDATE_PACKAGE_SCHEMA = "utd-browser-candidate-package/v1" as const;

export interface CandidateCoreFile {
  filename: string;
  content: string;
  mime: "application/json";
}

export interface CandidatePackageManifest {
  schema_version: typeof CANDIDATE_PACKAGE_SCHEMA;
  project_id: string;
  catalog_hash: string;
  generated_at: string;
  files: Array<{
    path: string;
    sha256: string;
    bytes: number;
  }>;
}

export interface CandidatePackage {
  filename: string;
  bytes: Uint8Array;
  manifest: CandidatePackageManifest;
}

/** Runtime-facing drafts travel beside the canonical project for an auditable candidate set. */
export function candidateCoreFiles(project: WorkbenchProject): CandidateCoreFile[] {
  return [
    { filename: "workbench.json", content: exportProjectJson(project), mime: "application/json" },
    { filename: "utd_block_transforms.json", content: exportBlockTransformsJson(project), mime: "application/json" },
    { filename: "utd_item_presentations.json", content: exportPresentationJson(project), mime: "application/json" }
  ];
}

/**
 * Builds one auditable ZIP. SHA-256 values are calculated with WebCrypto over
 * the exact UTF-8 bytes written into the archive, before the ZIP is offered.
 */
export async function buildCandidatePackage(
  project: WorkbenchProject,
  generatedAt = new Date().toISOString()
): Promise<CandidatePackage> {
  const files = candidateCoreFiles(project);
  const encoded = await Promise.all(files.map(async (file) => {
    const bytes = new TextEncoder().encode(file.content);
    return {
      ...file,
      bytes,
      sha256: await sha256Hex(bytes)
    };
  }));
  const manifest: CandidatePackageManifest = {
    schema_version: CANDIDATE_PACKAGE_SCHEMA,
    project_id: project.manifest.projectId,
    catalog_hash: project.manifest.catalogHash,
    generated_at: generatedAt,
    files: encoded.map((file) => ({
      path: file.filename,
      sha256: file.sha256,
      bytes: file.bytes.byteLength
    }))
  };
  const manifestText = stableStringify(manifest, 2) + "\n";
  const archive = new JSZip();
  const entryDate = new Date(generatedAt);
  const zipDate = Number.isNaN(entryDate.valueOf()) ? new Date(0) : entryDate;
  for (const file of encoded) archive.file(file.filename, file.bytes, { date: zipDate });
  archive.file("manifest.json", manifestText, { date: zipDate });
  const bytes = await archive.generateAsync({
    type: "uint8array",
    compression: "DEFLATE",
    compressionOptions: { level: 6 }
  });
  return {
    filename: `${safeFilename(project.manifest.projectId)}.candidate.zip`,
    bytes,
    manifest
  };
}

async function sha256Hex(bytes: Uint8Array): Promise<string> {
  const subtle = globalThis.crypto?.subtle;
  if (!subtle) throw new Error("当前浏览器不支持 WebCrypto SHA-256，候选包未生成。");
  const source = Uint8Array.from(bytes).buffer;
  const digest = new Uint8Array(await subtle.digest("SHA-256", source));
  return [...digest].map((value) => value.toString(16).padStart(2, "0")).join("");
}

function safeFilename(value: string): string {
  return value.replace(/[^a-zA-Z0-9_-]+/g, "_") || "utd-assets";
}
