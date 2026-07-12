import { createHash } from "node:crypto";
import { existsSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { describe, expect, it, vi } from "vitest";
import {
  BLOCK_TRANSFORM_DIFF_SCHEMA,
  blockTransformRulesFromSource,
  diffBlockTransformSources,
  exportBlockTransformDiffJson
} from "../src/domain/blockTransformDiff";
import {
  assertDistinctDiffOutputPaths,
  pathsEqualForPlatform,
  replaceAfterWindowsRenameConflict
} from "../cli/index";

const workbenchRoot = fileURLToPath(new URL("..", import.meta.url));
const cliPath = path.join(workbenchRoot, "cli", "index.ts");
const sampleProjectPath = path.join(workbenchRoot, "examples", "sample-output", "workbench.json");
const fingerprints = { baselineSha256: "a".repeat(64), candidateSha256: "b".repeat(64) };

describe("block transform release CLI", () => {
  it("reports deterministic additions, removals, field changes, and enable transitions", () => {
    const baseline = runtimeDocument([
      runtimeRule("utd:remove", { enabled: true }),
      runtimeRule("UTD:change", {
        target: { block: "minecraft:stone", state: { axis: "x", facing: "north" }, blockEntityPolicy: "reject" }
      }),
      runtimeRule("utd:disable", { enabled: true }),
      runtimeRule("utd:unchanged")
    ]);
    const candidate = runtimeDocument([
      runtimeRule("utd:unchanged"),
      runtimeRule("utd:add", { enabled: true }),
      runtimeRule("utd:change", {
        enabled: true,
        priority: 8,
        target: { block: "minecraft:granite", state: { facing: "south", axis: "y" }, blockEntityPolicy: "reject" },
        catalyst: {
          registryId: "minecraft:clay_ball",
          variantDiscriminator: "",
          componentsSnbt: "{}",
          count: 2,
          source: "inventory",
          consume: false
        },
        activation: { hand: "any", requireSneak: true, allowFakePlayer: true },
        result: { block: "minecraft:cobblestone", state: { waterlogged: "false" }, copyProperties: ["axis"] },
        creative: { requireInput: false, consume: false }
      }),
      runtimeRule("utd:disable")
    ]);

    const diff = diffBlockTransformSources(baseline, candidate, fingerprints);

    expect(diff.schema_version).toBe(BLOCK_TRANSFORM_DIFF_SCHEMA);
    expect(diff.summary).toEqual({
      baseline_sha256: "a".repeat(64),
      candidate_sha256: "b".repeat(64),
      baseline_rules: 4,
      candidate_rules: 4,
      baseline_enabled_rules: 2,
      candidate_enabled_rules: 2,
      added: 1,
      removed: 1,
      modified: 2,
      enabled: 1,
      disabled: 1,
      unchanged: 1
    });
    expect(diff.added.map((rule) => rule.id)).toEqual(["utd:add"]);
    expect(diff.removed.map((rule) => rule.id)).toEqual(["utd:remove"]);
    expect(diff.enabled).toEqual(["utd:change"]);
    expect(diff.disabled).toEqual(["utd:disable"]);
    expect(diff.modified[0].changed_fields).toEqual([
      "enabled",
      "priority",
      "target",
      "catalyst",
      "activation",
      "result",
      "creative"
    ]);
    expect(diff.modified[1]).toMatchObject({ id: "utd:disable", changed_fields: ["enabled"] });
    expect(exportBlockTransformDiffJson(diffBlockTransformSources(
      runtimeDocument([...baseline.rules].reverse()),
      runtimeDocument([...candidate.rules].reverse()),
      fingerprints
    ))).toBe(exportBlockTransformDiffJson(diff));
  });

  it("accepts a Workbench project as either side of a diff", () => {
    const project = sampleProject();
    project.blockTransforms = [workbenchRule("utd:from_workbench")];
    const added = diffBlockTransformSources(runtimeDocument([]), project, fingerprints);
    const removed = diffBlockTransformSources(project, runtimeDocument([]), fingerprints);

    expect(added.summary.added).toBe(1);
    expect(added.added[0]).toMatchObject({
      id: "utd:from_workbench",
      target: { block: "minecraft:stone" },
      catalyst: { registryId: "minecraft:stick" },
      result: { block: "minecraft:cobblestone" }
    });
    expect(removed.summary.removed).toBe(1);
    expect(removed.removed[0].id).toBe("utd:from_workbench");
  });

  it("strictly mirrors Java v1 types instead of repairing malformed runtime rules", () => {
    const invalidDocuments: Array<[string, (rule: any) => void, string]> = [
      ["enabled string", (rule) => { rule.enabled = "false"; }, "enabled: must be a boolean"],
      ["priority string", (rule) => { rule.priority = "100"; }, "priority: must be an integer"],
      ["fractional priority", (rule) => { rule.priority = 1.5; }, "priority: must be an integer"],
      ["non-string state", (rule) => { rule.target.state.axis = 2; }, "target.state.axis: must be a string"],
      ["non-object target", (rule) => { rule.target = []; }, "target: must be an object"],
      ["missing result", (rule) => { delete rule.result; }, "result: is required"],
      ["boolean string", (rule) => { rule.catalyst.consume = "true"; }, "consume: must be a boolean"],
      ["bad registry id", (rule) => { rule.result.block = "stone"; }, "invalid namespaced registry id"],
      ["bad rule id", (rule) => { rule.id = "bad id!"; }, "must be a legacy simple id"],
      ["bad state property", (rule) => { rule.target.state["bad key"] = "x"; }, "invalid block-state property name"],
      ["bad block entity policy", (rule) => { rule.target.blockEntityPolicy = "replace"; }, "v1 only supports reject"],
      ["bad source enum", (rule) => { rule.catalyst.source = "hand"; }, "expected clicked_hand or inventory"],
      ["bad hand enum", (rule) => { rule.activation.hand = "both"; }, "expected main, off or any"],
      ["non-string copy property", (rule) => { rule.result.copyProperties = [2]; }, "must be a string"],
      ["duplicate copy property", (rule) => { rule.result.copyProperties = ["Axis", " axis "]; }, "duplicate property axis"],
      ["unsafe creative relation", (rule) => { rule.creative = { requireInput: false, consume: true }; }, "consume=true requires requireInput=true"]
    ];

    for (const [label, mutate, error] of invalidDocuments) {
      const document = runtimeDocument([runtimeRule(`utd:${label.replaceAll(" ", "_")}`)]);
      mutate(document.rules[0]);
      expect(() => blockTransformRulesFromSource(document), label).toThrow(error);
    }

    expect(() => blockTransformRulesFromSource({
      schema_version: "utd-block-transforms/v1",
      rules: {}
    })).toThrow("$.rules: must be an array");
    expect(() => blockTransformRulesFromSource({
      schema_version: 1,
      rules: []
    })).toThrow("$.schema_version: must be a string");

    expect(() => blockTransformRulesFromSource(runtimeDocument([
      runtimeRule("UTD:Same"),
      runtimeRule("utd:same")
    ]))).toThrow("duplicate rule id utd:same");
  });

  it("normalizes runtime values exactly as the Java parser does", () => {
    const rule = runtimeRule("  UTD:Normalize  ", {
      target: { block: " Minecraft:Stone ", state: { " Axis ": " Y " }, blockEntityPolicy: " REJECT " },
      catalyst: {
        registryId: " Minecraft:Stick ",
        variantDiscriminator: "  custom  ",
        componentsSnbt: "   ",
        count: 1,
        source: " INVENTORY ",
        consume: true
      },
      activation: { hand: " ANY ", requireSneak: false, allowFakePlayer: false },
      result: {
        block: " Minecraft:Cobblestone ",
        state: { Waterlogged: " FALSE " },
        copyProperties: [" Axis "]
      }
    });

    expect(blockTransformRulesFromSource(runtimeDocument([rule]))[0]).toMatchObject({
      id: "utd:normalize",
      target: { block: "minecraft:stone", state: { axis: "y" }, blockEntityPolicy: "reject" },
      catalyst: {
        registryId: "minecraft:stick",
        variantDiscriminator: "custom",
        componentsSnbt: "{}",
        source: "inventory"
      },
      activation: { hand: "any" },
      result: {
        block: "minecraft:cobblestone",
        state: { waterlogged: "false" },
        copyProperties: ["axis"]
      }
    });
  });

  it("limits validate failures to block-transform errors when requested", () => {
    withTempDirectory((directory) => {
      const recipeOnly = sampleProject();
      recipeOnly.issues.push({
        severity: "error",
        code: "recipe_cycle",
        entityType: "recipe",
        entityId: "cycle:test",
        message: "Existing recipe problem"
      });
      const recipeOnlyPath = writeJson(directory, "recipe-only.json", recipeOnly);

      expect(runCli("validate", "--project", recipeOnlyPath).status).toBe(2);
      const scoped = runCli("validate", "--project", recipeOnlyPath, "--only", "block_transform");
      expect(scoped.status).toBe(0);
      expect(scoped.stderr).not.toContain("recipe_cycle");

      const blockError = structuredClone(recipeOnly);
      blockError.blockTransforms = [workbenchRule("utd:invalid", { clickedBlock: "stone" })];
      const blockErrorPath = writeJson(directory, "block-error.json", blockError);
      const blocked = runCli("validate", "--project", blockErrorPath, "--only", "block_transform");
      expect(blocked.status).toBe(2);
      expect(blocked.stderr).toContain("block_transform_invalid_target_id");
      expect(blocked.stderr).not.toContain("recipe_cycle");
    });
  });

  it("writes a JSON diff from a runtime baseline to a Workbench candidate", () => {
    withTempDirectory((directory) => {
      const baselinePath = writeJson(directory, "baseline.json", runtimeDocument([]));
      const candidate = sampleProject();
      candidate.blockTransforms = [workbenchRule("utd:release")];
      const candidatePath = writeJson(directory, "candidate.json", candidate);
      const outPath = path.join(directory, "diff.json");

      const result = runCli(
        "diff-block-transforms",
        "--baseline", baselinePath,
        "--candidate", candidatePath,
        "--out", outPath
      );

      expect(result.status).toBe(0);
      expect(result.stdout).toContain("+1 -0 ~0");
      const candidateSha256 = sha256(readFileSync(candidatePath, "utf8"));
      expect(result.stdout).toContain(`candidate sha256=${candidateSha256}`);
      expect(result.stdout).toContain("candidate rules=1 enabled=1");
      const diff = JSON.parse(readFileSync(outPath, "utf8"));
      expect(diff).toMatchObject({
        schema_version: BLOCK_TRANSFORM_DIFF_SCHEMA,
        summary: {
          baseline_sha256: sha256(readFileSync(baselinePath, "utf8")),
          candidate_sha256: candidateSha256,
          baseline_rules: 0,
          candidate_rules: 1,
          baseline_enabled_rules: 0,
          candidate_enabled_rules: 1,
          added: 1,
          removed: 0,
          modified: 0
        },
        added: [{ id: "utd:release" }]
      });
    });
  });

  it("rejects an output path that resolves to either input before writing", async () => {
    await withTempDirectoryAsync(async (directory) => {
      const baselinePath = writeJson(directory, "baseline.json", runtimeDocument([]));
      const candidatePath = writeJson(directory, "candidate.json", runtimeDocument([]));
      const originalCandidate = readFileSync(candidatePath, "utf8");
      const result = runCli(
        "diff-block-transforms",
        "--baseline", baselinePath,
        "--candidate", candidatePath,
        "--out", path.join(directory, ".", "candidate.json")
      );

      expect(result.status).toBe(1);
      expect(result.stderr).toContain("--out must not overwrite --candidate");
      expect(readFileSync(candidatePath, "utf8")).toBe(originalCandidate);
      expect(pathsEqualForPlatform("C:\\UTD\\Candidate.JSON", "c:\\utd\\candidate.json", "win32")).toBe(true);
      await expect(assertDistinctDiffOutputPaths(
        baselinePath,
        candidatePath,
        candidatePath.toUpperCase(),
        "win32"
      )).rejects.toThrow("--candidate");
      await expect(assertDistinctDiffOutputPaths(
        baselinePath,
        candidatePath,
        baselinePath.toUpperCase(),
        "win32"
      )).rejects.toThrow("--baseline");
    });
  });

  it("makes malformed runtime input fail the CLI without producing a diff", () => {
    withTempDirectory((directory) => {
      const baselinePath = writeJson(directory, "baseline.json", runtimeDocument([]));
      const malformed = runtimeDocument([runtimeRule("utd:malformed")]);
      malformed.rules[0].enabled = "false";
      const candidatePath = writeJson(directory, "candidate.json", malformed);
      const outPath = path.join(directory, "diff.json");
      const result = runCli(
        "diff-block-transforms",
        "--baseline", baselinePath,
        "--candidate", candidatePath,
        "--out", outPath
      );

      expect(result.status).toBe(1);
      expect(result.stderr).toContain("enabled: must be a boolean");
      expect(existsSync(outPath)).toBe(false);
    });
  });

  it("restores the backup when Windows fallback placement fails", async () => {
    const target = "target.json";
    const temp = "temp.json";
    const backup = "backup.json";
    const placementError = Object.assign(new Error("placement failed"), { code: "EPERM" });
    const renameOperation = vi.fn(async (from: string, to: string) => {
      if (from === temp && to === target) throw placementError;
    });
    const removeOperation = vi.fn(async () => undefined);

    await expect(replaceAfterWindowsRenameConflict(target, temp, backup, {
      rename: renameOperation,
      rm: removeOperation
    })).rejects.toBe(placementError);

    expect(renameOperation.mock.calls).toEqual([
      [target, backup],
      [temp, target],
      [backup, target]
    ]);
    expect(removeOperation).toHaveBeenCalledOnce();
    expect(removeOperation).toHaveBeenCalledWith(temp, { force: true });
    expect(removeOperation).not.toHaveBeenCalledWith(backup, { force: true });
  });

  it("retains the backup when restoration itself fails and only deletes it after success", async () => {
    const target = "target.json";
    const temp = "temp.json";
    const backup = "backup.json";
    const failedRemove = vi.fn(async () => undefined);
    let renameStep = 0;
    await expect(replaceAfterWindowsRenameConflict(target, temp, backup, {
      rename: async () => {
        renameStep += 1;
        if (renameStep >= 2) throw new Error(renameStep === 2 ? "placement failed" : "restore failed");
      },
      rm: failedRemove
    })).rejects.toThrow(`Backup retained at ${backup}`);
    expect(failedRemove).not.toHaveBeenCalledWith(backup, { force: true });

    const successfulRemove = vi.fn(async () => undefined);
    await replaceAfterWindowsRenameConflict(target, temp, backup, {
      rename: async () => undefined,
      rm: successfulRemove
    });
    expect(successfulRemove.mock.calls).toEqual([
      [backup, { force: true }],
      [temp, { force: true }]
    ]);
  });
});

function runtimeDocument(rules: Array<Record<string, unknown>>) {
  return { schema_version: "utd-block-transforms/v1", rules };
}

function runtimeRule(id: string, patch: Record<string, unknown> = {}) {
  return {
    id,
    enabled: false,
    priority: 0,
    target: { block: "minecraft:stone", state: {}, blockEntityPolicy: "reject" },
    catalyst: {
      registryId: "minecraft:stick",
      variantDiscriminator: "",
      componentsSnbt: "{}",
      count: 1,
      source: "clicked_hand",
      consume: true
    },
    activation: { hand: "main", requireSneak: false, allowFakePlayer: false },
    result: { block: "minecraft:cobblestone", state: {}, copyProperties: [] },
    creative: { requireInput: true, consume: false },
    ...patch
  };
}

function workbenchRule(id: string, patch: Record<string, unknown> = {}) {
  return {
    id,
    enabled: true,
    priority: 0,
    clickedBlock: "minecraft:stone",
    targetState: {},
    resultBlock: "minecraft:cobblestone",
    resultState: {},
    copyProperties: [],
    catalyst: { refKind: "item", ref: "minecraft:stick", count: 1 },
    catalystComponentsSnbt: "{}",
    inputSource: "clicked_hand",
    hand: "main",
    requireSneaking: false,
    allowFakePlayer: false,
    consumeInput: true,
    cancelInteraction: true,
    blockEntityPolicy: "reject",
    creativeRequireInput: true,
    creativeConsume: false,
    ...patch
  };
}

function sampleProject(): any {
  return JSON.parse(readFileSync(sampleProjectPath, "utf8"));
}

function runCli(...args: string[]) {
  return spawnSync(process.execPath, ["--import", "tsx", cliPath, ...args], {
    cwd: workbenchRoot,
    encoding: "utf8"
  });
}

function writeJson(directory: string, name: string, value: unknown): string {
  const target = path.join(directory, name);
  writeFileSync(target, JSON.stringify(value), "utf8");
  return target;
}

function withTempDirectory(run: (directory: string) => void): void {
  const directory = mkdtempSync(path.join(tmpdir(), "utd-block-transform-cli-"));
  try {
    run(directory);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

async function withTempDirectoryAsync(run: (directory: string) => Promise<void>): Promise<void> {
  const directory = mkdtempSync(path.join(tmpdir(), "utd-block-transform-cli-"));
  try {
    await run(directory);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

function sha256(value: string): string {
  return createHash("sha256").update(value, "utf8").digest("hex");
}
