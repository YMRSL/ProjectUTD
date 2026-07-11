import type { Dispatch, SetStateAction } from "react";
import { updateManagedItem, updateManagedLoot, updateManagedRecipe } from "../domain/mutations";
import type { CanonicalItem, CanonicalRecipe, WorkbenchProject } from "../domain/schema";
import { lootMatchesItem, refMatchesItem } from "../domain/relations";
import { stableStringify } from "../domain/stable";
import type { EntitySelection } from "./GraphCanvas";

export type InspectorTab = "record" | "recipe" | "loot" | "issues";

interface Props {
  project: WorkbenchProject;
  rootItemKey: string;
  selection: EntitySelection;
  tab: InspectorTab;
  onTab: (tab: InspectorTab) => void;
  onSelection: (selection: EntitySelection) => void;
  setProject: Dispatch<SetStateAction<WorkbenchProject>>;
}

export function Inspector({ project, rootItemKey, selection, tab, onTab, onSelection, setProject }: Props) {
  const rootItem = project.items.find((item) => item.itemKey === rootItemKey)!;
  const selectedItem = selection.kind === "item" ? project.items.find((item) => item.itemKey === selection.id) : undefined;
  const contextItem = selectedItem ?? rootItem;
  const recipes = recipesFor(project, rootItem);
  const selectedRecipe = selection.kind === "recipe"
    ? project.recipes.find((recipe) => recipe.id === selection.id)
    : recipes[0];
  const loot = project.lootPolicies.filter((policy) => lootMatchesItem(policy, rootItem));
  const issueCount = relevantIssues(project, contextItem, recipes).length;

  return (
    <aside className="inspector">
      <div className="inspector-tabs" role="tablist" aria-label="检查器">
        <Tab active={tab === "record"} onClick={() => onTab("record")}>档案</Tab>
        <Tab active={tab === "recipe"} count={recipes.length} onClick={() => onTab("recipe")}>配方</Tab>
        <Tab active={tab === "loot"} count={loot.length} onClick={() => onTab("loot")}>Loot</Tab>
        <Tab active={tab === "issues"} count={issueCount} onClick={() => onTab("issues")}>问题</Tab>
      </div>

      <div className="inspector-scroll">
        {tab === "record" && (
          <RecordPanel
            item={contextItem}
            onChange={(patch) => setProject((current) => updateManagedItem(current, contextItem.itemKey, patch))}
          />
        )}
        {tab === "recipe" && (
          <RecipePanel
            recipes={recipes}
            selected={selectedRecipe}
            onSelect={(recipe) => onSelection({ kind: "recipe", id: recipe.id })}
            onChange={(recipeId, patch) => setProject((current) => updateManagedRecipe(current, recipeId, patch))}
          />
        )}
        {tab === "loot" && (
          <LootPanel
            root={rootItem}
            policies={loot}
            onChange={(identityKey, patch) => setProject((current) => updateManagedLoot(current, identityKey, patch))}
          />
        )}
        {tab === "issues" && <IssuesPanel project={project} item={contextItem} recipes={recipes} />}
      </div>
    </aside>
  );
}

function Tab({ active, count, onClick, children }: { active: boolean; count?: number; onClick: () => void; children: React.ReactNode }) {
  return (
    <button type="button" role="tab" aria-selected={active} className={active ? "is-active" : ""} onClick={onClick}>
      {children}{count !== undefined && <span>{count}</span>}
    </button>
  );
}

function RecordPanel({ item, onChange }: { item: CanonicalItem; onChange: (patch: Partial<Pick<CanonicalItem, "clientNameZhCn" | "translationKey">>) => void }) {
  const editable = item.ownership === "utd" && item.managed;
  return (
    <div className="inspector-panel">
      <PanelKicker>{item.humanSelected ? "HUMAN SELECTED" : item.managed ? "UTD CATALOG" : "DEPENDENCY LEAF"}</PanelKicker>
      <h2>{item.clientNameZhCn}</h2>
      <p className="mono inspector-id">{item.registryId}</p>
      <div className={`edit-boundary ${editable ? "is-editable" : "is-locked"}`}>
        <span>{editable ? "UTD OWNED · 可编辑" : "EXTERNAL · 只读"}</span>
        <small>{editable ? "修改会进入 pending 状态" : "外部依赖不会被导出器改写"}</small>
      </div>

      <SectionTitle>客户端目录</SectionTitle>
      <Field label="中文名称">
        <input value={item.clientNameZhCn} disabled={!editable} onChange={(event) => onChange({ clientNameZhCn: event.target.value })} />
      </Field>
      <Field label="Translation key">
        <input className="mono" value={item.translationKey} disabled={!editable} placeholder="缺失" onChange={(event) => onChange({ translationKey: event.target.value })} />
      </Field>

      <SectionTitle>状态回写</SectionTitle>
      <dl className="status-ledger">
        <Status label="managed" value={item.managed ? "YES" : "NO"} tone={item.managed ? "good" : "muted"} />
        <Status label="human_selected" value={item.humanSelected ? "YES" : "NO"} tone={item.humanSelected ? "accent" : "muted"} />
        <Status label="recipe_input" value={String(item.recipeInputCount)} tone={item.recipeInput ? "good" : "muted"} />
        <Status label="recipe_output" value={String(item.recipeOutputCount)} tone={item.recipeOutput ? "good" : "muted"} />
        <Status label="loot_level" value={item.lootLevel === null ? "—" : `L${item.lootLevel}`} tone={item.lootEnabled ? "accent" : "muted"} />
        <Status label="sync" value={item.sync.toUpperCase()} tone={syncTone(item.sync)} />
      </dl>

      <SectionTitle>规范身份</SectionTitle>
      <KeyValue label="item_key" value={item.itemKey} />
      <KeyValue label="identity_kind" value={item.identityKind} />
      <KeyValue label="variant_key" value={item.variantKey ?? "—"} />
      <KeyValue label="discriminator" value={item.variantDiscriminator || "—"} />
      <KeyValue label="source" value={item.source} />
      <KeyValue label="catalog_hash" value={item.catalogHash} />
      <KeyValue label="deployed_hash" value={item.deployedHash ?? "—"} />
      <details className="raw-details">
        <summary>Canonical components / variant</summary>
        <pre>{stableStringify({
          variant: item.canonicalVariant,
          canonical_components: item.canonicalComponents,
          components_canonical: item.componentsCanonical,
          components_snbt: item.componentsSnbt,
          identity_components_canonical: item.identityComponentsCanonical
        }, 2)}</pre>
      </details>
    </div>
  );
}

function RecipePanel({
  recipes,
  selected,
  onSelect,
  onChange
}: {
  recipes: CanonicalRecipe[];
  selected?: CanonicalRecipe;
  onSelect: (recipe: CanonicalRecipe) => void;
  onChange: (id: string, patch: Partial<Pick<CanonicalRecipe, "station" | "stationKey" | "stationScope" | "form" | "level">>) => void;
}) {
  if (!recipes.length) return <EmptyPanel code="NO_RECIPE" text="当前白名单根没有配方产出。" />;
  const recipe = selected && recipes.some((entry) => entry.id === selected.id) ? selected : recipes[0];
  return (
    <div className="inspector-panel">
      <PanelKicker>OUTPUT RECIPES · {recipes.length}</PanelKicker>
      {recipes.length > 1 && (
        <div className="recipe-switcher">
          {recipes.map((entry, index) => (
            <button type="button" className={entry.id === recipe.id ? "is-active" : ""} key={entry.id} onClick={() => onSelect(entry)}>
              {String(index + 1).padStart(2, "0")} · {entry.station}
            </button>
          ))}
        </div>
      )}
      <h2>{recipe.outputName || "未命名配方"}</h2>
      <p className="mono inspector-id">{recipe.id}</p>
      <div className={`edit-boundary ${recipe.editable ? "is-editable" : "is-locked"}`}>
        <span>{recipe.editable ? "UTD RECIPE · 可编辑" : "EXTERNAL RECIPE · 只读"}</span>
        <small>{recipe.recipeType}</small>
      </div>
      <SectionTitle>工艺参数</SectionTitle>
      <Field label="工作站">
        <input value={recipe.station} disabled={!recipe.editable} onChange={(event) => onChange(recipe.id, { station: event.target.value })} />
      </Field>
      <div className="field-pair">
        <Field label="等级">
          <input type="number" min="0" max="9" value={recipe.level ?? ""} disabled={!recipe.editable} onChange={(event) => onChange(recipe.id, { level: event.target.value === "" ? null : Number(event.target.value) })} />
        </Field>
        <Field label="形态">
          <input value={recipe.form} disabled={!recipe.editable} onChange={(event) => onChange(recipe.id, { form: event.target.value })} />
        </Field>
      </div>
      <KeyValue label="station_key" value={recipe.stationKey} />
      <KeyValue label="station_scope" value={recipe.stationScope} />
      <KeyValue label="source" value={`${recipe.sheet}:${recipe.sourceRow ?? "generated"}`} />

      <SectionTitle>投入 / 产出</SectionTitle>
      <div className="material-ledger">
        {recipe.inputs.map((input, index) => (
          <div key={`${input.refKind}:${input.ref}:${index}`}>
            <span className={`ref-kind ref-kind--${input.refKind}`}>{input.refKind}</span>
            <code>{input.refKind === "tag" ? "#" : ""}{input.ref}</code>
            <strong>×{input.count}</strong>
          </div>
        ))}
        <div className="material-arrow">↓ OUTPUT</div>
        {recipe.outputs.map((output, index) => (
          <div className="is-output" key={`${output.refKind}:${output.ref}:${index}`}>
            <span className={`ref-kind ref-kind--${output.refKind}`}>{output.refKind}</span>
            <code>{output.ref}</code>
            <strong>×{output.count}</strong>
          </div>
        ))}
      </div>
      {recipe.pattern && (
        <div className="pattern-grid" aria-label="有序配方图案">
          {recipe.pattern.flatMap((row, rowIndex) => [...row.padEnd(3)].map((symbol, columnIndex) => (
            <span key={`${rowIndex}-${columnIndex}`}>{symbol.trim() || "·"}</span>
          )))}
        </div>
      )}
      <details className="raw-details">
        <summary>原始配方载荷（保留导出）</summary>
        <pre>{stableStringify(recipe.raw, 2)}</pre>
      </details>
    </div>
  );
}

function LootPanel({ root, policies, onChange }: {
  root: CanonicalItem;
  policies: WorkbenchProject["lootPolicies"];
  onChange: (id: string, patch: Partial<Pick<CanonicalLootPolicyShape, "lootEnabled" | "level" | "count" | "commonTags" | "commonBaseWeight" | "directedWeight" | "replacePriority">>) => void;
}) {
  if (!policies.length) return <EmptyPanel code="NO_LOOT_POLICY" text="当前身份未匹配 Loot registry 条目。" />;
  const editable = root.ownership === "utd" && root.managed;
  return (
    <div className="inspector-panel">
      <PanelKicker>LOOT REGISTRY · {policies.length}</PanelKicker>
      <h2>{root.clientNameZhCn}</h2>
      <p className="panel-note">同一 registry id 的变体分条保留；此处不会合并或改写原始 NBT 身份。</p>
      {policies.map((policy, index) => (
        <section className="loot-policy" key={policy.identityKey}>
          <div className="loot-policy__head">
            <span>ENTRY {String(index + 1).padStart(2, "0")}</span>
            <label className="switch">
              <input type="checkbox" checked={policy.lootEnabled} disabled={!editable} onChange={(event) => onChange(policy.identityKey, { lootEnabled: event.target.checked })} />
              <i />
              {policy.lootEnabled ? "启用" : "停用"}
            </label>
          </div>
          <p className="mono loot-identity">{policy.identityKey}</p>
          <div className="field-pair">
            <Field label="Loot 等级">
              <input type="number" min="0" max="5" value={policy.level} disabled={!editable} onChange={(event) => onChange(policy.identityKey, { level: Number(event.target.value) })} />
            </Field>
            <Field label="数量">
              <input type="number" min="1" value={policy.count} disabled={!editable} onChange={(event) => onChange(policy.identityKey, { count: Number(event.target.value) })} />
            </Field>
          </div>
          <div className="field-pair">
            <Field label="通用权重">
              <input type="number" min="0" value={policy.commonBaseWeight} disabled={!editable} onChange={(event) => onChange(policy.identityKey, { commonBaseWeight: Number(event.target.value) })} />
            </Field>
            <Field label="定向权重">
              <input type="number" min="0" value={policy.directedWeight} disabled={!editable} onChange={(event) => onChange(policy.identityKey, { directedWeight: Number(event.target.value) })} />
            </Field>
          </div>
          <Field label="通用标签（逗号分隔）">
            <input value={policy.commonTags.join(", ")} disabled={!editable} onChange={(event) => onChange(policy.identityKey, { commonTags: event.target.value.split(",").map((value) => value.trim()).filter(Boolean) })} />
          </Field>
          <KeyValue label="allowed templates" value={policy.allowedCommonTemplates.join(" / ") || "—"} />
          <KeyValue label="directed templates" value={policy.directedTemplates.join(" / ") || "—"} />
        </section>
      ))}
    </div>
  );
}

type CanonicalLootPolicyShape = WorkbenchProject["lootPolicies"][number];

function IssuesPanel({ project, item, recipes }: { project: WorkbenchProject; item: CanonicalItem; recipes: CanonicalRecipe[] }) {
  const issues = relevantIssues(project, item, recipes);
  if (!issues.length) return <EmptyPanel code="CLEAR" text="当前检查范围没有结构性问题。" success />;
  return (
    <div className="inspector-panel">
      <PanelKicker>VALIDATION LOG · {issues.length}</PanelKicker>
      <h2>需要处理</h2>
      <div className="issue-log">
        {issues.map((issue, index) => (
          <article key={`${issue.code}-${issue.entityId}-${index}`} className={`issue issue--${issue.severity}`}>
            <span>{issue.severity.toUpperCase()}</span>
            <strong>{issue.code}</strong>
            <p>{issue.message}</p>
            <code>{issue.entityId}</code>
          </article>
        ))}
      </div>
    </div>
  );
}

function recipesFor(project: WorkbenchProject, item: CanonicalItem): CanonicalRecipe[] {
  return project.recipes.filter((recipe) => recipe.outputs.some((output) => refMatchesItem(output, item)));
}

function relevantIssues(project: WorkbenchProject, item: CanonicalItem, recipes: CanonicalRecipe[]) {
  const recipeIds = new Set(recipes.map((recipe) => recipe.id));
  return project.issues.filter((issue) => issue.entityId === item.itemKey || (issue.entityType === "recipe" && recipeIds.has(issue.entityId)) || issue.entityId.split("|").includes(item.itemKey));
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <label className="field"><span>{label}</span>{children}</label>;
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h3 className="section-title"><span>{children}</span></h3>;
}

function PanelKicker({ children }: { children: React.ReactNode }) {
  return <div className="panel-kicker">{children}</div>;
}

function KeyValue({ label, value }: { label: string; value: string }) {
  return <div className="key-value"><span>{label}</span><code>{value}</code></div>;
}

function Status({ label, value, tone }: { label: string; value: string; tone: string }) {
  return <div><dt>{label}</dt><dd className={`tone-${tone}`}>{value}</dd></div>;
}

function syncTone(sync: CanonicalItem["sync"]): string {
  if (sync === "synced") return "good";
  if (sync === "pending" || sync === "stale") return "accent";
  if (sync === "error") return "bad";
  return "muted";
}

function EmptyPanel({ code, text, success = false }: { code: string; text: string; success?: boolean }) {
  return <div className={`empty-panel ${success ? "is-success" : ""}`}><strong>{code}</strong><p>{text}</p></div>;
}
