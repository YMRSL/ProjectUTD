import { useEffect, useMemo, useState, type Dispatch, type SetStateAction } from "react";
import {
  formatBlockStateInput,
  parseBlockStateInput,
  parseCopyPropertiesInput,
  validateBlockTransformIdInput
} from "../domain/authoringInputs";
import { describeBlockTransformIssueZhCn, normalizeBlockTransformRuleId } from "../domain/blockTransforms";
import {
  addBlockTransform,
  removeBlockTransform,
  updateBlockTransform,
  updateItemPresentation,
  updateManagedLoot,
  updateManagedRecipe
} from "../domain/mutations";
import { defaultPresentation, isFpePresentationVariant, presentationForItem } from "../domain/presentation";
import type {
  BlockTransform,
  CanonicalItem,
  CanonicalRecipe,
  ItemPresentationOverride,
  ValidationIssue,
  WorkbenchProject
} from "../domain/schema";
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
  const presentation = presentationForItem(project, contextItem) ?? defaultPresentation(contextItem);
  const transforms = transformsFor(project, contextItem);
  const issueCount = relevantIssues(project, contextItem, recipes).length;

  return (
    <aside className="inspector">
      <div className="inspector-tabs" role="tablist" aria-label="检查器">
        <Tab active={tab === "record"} onClick={() => onTab("record")}>档案</Tab>
        <Tab active={tab === "recipe"} count={recipes.length + transforms.length} onClick={() => onTab("recipe")}>配方</Tab>
        <Tab active={tab === "loot"} count={loot.length} onClick={() => onTab("loot")}>Loot</Tab>
        <Tab active={tab === "issues"} count={issueCount} onClick={() => onTab("issues")}>问题</Tab>
      </div>

      <div className="inspector-scroll">
        {tab === "record" && (
          <RecordPanel
            item={contextItem}
            presentation={presentation}
            onPresentationChange={(patch) => setProject((current) => updateItemPresentation(current, contextItem.itemKey, patch))}
          />
        )}
        {tab === "recipe" && (
          <RecipePanel
            recipes={recipes}
            selected={selectedRecipe}
            catalyst={contextItem}
            transforms={transforms}
            validationIssues={project.issues.filter((issue) => issue.entityType === "block_transform")}
            onSelect={(recipe) => onSelection({ kind: "recipe", id: recipe.id })}
            onChange={(recipeId, patch) => setProject((current) => updateManagedRecipe(current, recipeId, patch))}
            onAddTransform={() => setProject((current) => addBlockTransform(current, contextItem.itemKey))}
            onTransformChange={(id, patch) => setProject((current) => updateBlockTransform(current, id, patch))}
            onRemoveTransform={(id) => setProject((current) => removeBlockTransform(current, id))}
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

function RecordPanel({ item, presentation, onPresentationChange }: {
  item: CanonicalItem;
  presentation: ItemPresentationOverride;
  onPresentationChange: (patch: Partial<Pick<ItemPresentationOverride, "enabled" | "nameZhCn" | "descriptionZhCn" | "applyScope">>) => void;
}) {
  const editable = item.ownership === "utd" && item.managed;
  const fpeVariant = isFpePresentationVariant(item);
  return (
    <div className="inspector-panel">
      <PanelKicker>{item.humanSelected ? "HUMAN SELECTED" : item.managed ? "UTD CATALOG" : "DEPENDENCY LEAF"}</PanelKicker>
      <h2>{item.clientNameZhCn}</h2>
      <p className="mono inspector-id">{item.registryId}</p>
      {item.iconDataUrl && <div className="inspector-item-icon"><img src={item.iconDataUrl} alt="" /></div>}
      <div className={`edit-boundary ${editable ? "is-editable" : "is-locked"}`}>
        <span>{editable ? "UTD OWNED · 可编辑" : "EXTERNAL · 只读"}</span>
        <small>{editable ? "修改会进入 pending 状态" : "外部依赖不会被导出器改写"}</small>
      </div>

      <SectionTitle>游戏观察值（只读证据）</SectionTitle>
      <KeyValue label="客户端中文名" value={item.clientNameZhCn || "—"} />
      <KeyValue label="translation_key" value={item.translationKey || "—"} />
      <KeyValue label="表格分类" value={`${item.categoryLabelZhCn}${item.categoryLevel === null ? "" : ` · L${item.categoryLevel}`}`} />

      <SectionTitle>显示覆盖</SectionTitle>
      <p className="panel-note">这里只记录待发布的语言覆盖，不会改写游戏导出的原始观察值，也不会直接写入运行目录。</p>
      <label className="switch presentation-switch">
        <input
          type="checkbox"
          checked={presentation.enabled}
          disabled={!editable}
          onChange={(event) => onPresentationChange({ enabled: event.target.checked })}
        />
        <i />
        {presentation.enabled ? "启用覆盖" : "保留草稿"}
      </label>
      <Field label="应用范围">
        <select
          value={presentation.applyScope}
          disabled={!editable || !item.variantDiscriminator || fpeVariant}
          onChange={(event) => onPresentationChange({ applyScope: event.target.value as ItemPresentationOverride["applyScope"] })}
        >
          <option value="identity">当前精确身份</option>
          <option value="registry">整个 registry 物品</option>
        </select>
      </Field>
      {fpeVariant && <p className="panel-note">FPE 食品固定按当前 food_id 精确覆盖，不能改写全部 pack_food。</p>}
      <Field label="游戏内中文名称">
        <input value={presentation.nameZhCn} disabled={!editable} onChange={(event) => onPresentationChange({ nameZhCn: event.target.value })} />
      </Field>
      <Field label="物品介绍（可多行）">
        <textarea
          rows={5}
          value={presentation.descriptionZhCn}
          disabled={!editable}
          placeholder="每行都会原样保留在语言覆盖值中"
          onChange={(event) => onPresentationChange({ descriptionZhCn: event.target.value })}
        />
      </Field>
      <KeyValue label="name key" value={presentation.nameKey} />
      <KeyValue label="tooltip key" value={presentation.descriptionKey} />

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
  catalyst,
  transforms,
  validationIssues,
  onSelect,
  onChange,
  onAddTransform,
  onTransformChange,
  onRemoveTransform
}: {
  recipes: CanonicalRecipe[];
  selected?: CanonicalRecipe;
  catalyst: CanonicalItem;
  transforms: BlockTransform[];
  validationIssues: ValidationIssue[];
  onSelect: (recipe: CanonicalRecipe) => void;
  onChange: (id: string, patch: Partial<Pick<CanonicalRecipe, "station" | "stationKey" | "stationScope" | "form" | "level">>) => void;
  onAddTransform: () => void;
  onTransformChange: (id: string, patch: Partial<Pick<BlockTransform,
    "id" | "enabled" | "priority" | "clickedBlock" | "targetState" | "resultBlock" | "resultState" | "copyProperties"
    | "inputSource" | "hand" | "requireSneaking" | "allowFakePlayer" | "consumeInput"
    | "creativeRequireInput" | "creativeConsume"
  > & { catalystCount: number }>) => void;
  onRemoveTransform: (id: string) => void;
}) {
  const recipe = recipes.length
    ? selected && recipes.some((entry) => entry.id === selected.id) ? selected : recipes[0]
    : undefined;
  const editableCatalyst = catalyst.ownership === "utd" && catalyst.managed;
  const blockingIssues = validationIssues.filter((issue) => issue.severity === "error");
  return (
    <div className="inspector-panel">
      <PanelKicker>OUTPUT RECIPES · {recipes.length}</PanelKicker>
      {!recipe && <EmptyPanel code="NO_RECIPE" text="当前白名单根没有普通配方产出；仍可在下方建立方块替换规则。" />}
      {recipe && recipes.length > 1 && (
        <div className="recipe-switcher">
          {recipes.map((entry, index) => (
            <button type="button" className={entry.id === recipe.id ? "is-active" : ""} key={entry.id} onClick={() => onSelect(entry)}>
              {String(index + 1).padStart(2, "0")} · {entry.station}
            </button>
          ))}
        </div>
      )}
      {recipe && <RecipeDetails recipe={recipe} onChange={onChange} />}

      <SectionTitle>方块替换制造</SectionTitle>
      <p className="panel-note">右键目标方块时取消原交互，消耗 catalyst 并替换为结果方块。规则这里只生成草稿数据。</p>
      {blockingIssues.length > 0 && (
        <div className="rule-validation is-blocking" role="alert">
          <strong>候选包已被 {blockingIssues.length} 个方块替换错误阻止</strong>
          <span>请修复下方对应规则；相同目标与优先级的跨规则冲突也会在各条规则中显示。</span>
        </div>
      )}
      <button type="button" className="button button--primary transform-add" disabled={!editableCatalyst} onClick={onAddTransform}>
        新建方块替换规则
      </button>
      {transforms.map((rule, index) => (
        <section className="loot-policy block-transform" key={`${rule.catalyst.ref}:${index}`}>
          <div className="loot-policy__head">
            <span>TRANSFORM {String(index + 1).padStart(2, "0")}</span>
            <label className="switch">
              <input type="checkbox" checked={rule.enabled} onChange={(event) => onTransformChange(rule.id, { enabled: event.target.checked })} />
              <i />
              {rule.enabled ? "启用" : "草稿"}
            </label>
          </div>
          <RuleIdField
            rule={rule}
            siblingIds={transforms.filter((entry) => entry !== rule).map((entry) => entry.id)}
            onCommit={(id) => onTransformChange(rule.id, { id })}
          />
          <Field label="目标方块（被右键）">
            <input className="mono" value={rule.clickedBlock} placeholder="minecraft:stone" onChange={(event) => onTransformChange(rule.id, { clickedBlock: event.target.value.trim() })} />
          </Field>
          <BlockStateField
            label="目标方块状态（JSON 对象）"
            value={rule.targetState}
            onChange={(value) => onTransformChange(rule.id, { targetState: value })}
          />
          <Field label="结果方块（替换后）">
            <input className="mono" value={rule.resultBlock} placeholder="minecraft:cobblestone" onChange={(event) => onTransformChange(rule.id, { resultBlock: event.target.value.trim() })} />
          </Field>
          <BlockStateField
            label="结果方块状态（JSON 对象）"
            value={rule.resultState}
            onChange={(value) => onTransformChange(rule.id, { resultState: value })}
          />
          <CommaListField
            label="复制状态属性（逗号分隔）"
            value={rule.copyProperties}
            placeholder="facing, waterlogged"
            onChange={(value) => onTransformChange(rule.id, { copyProperties: value })}
          />
          <div className="field-pair">
            <Field label="消耗数量">
              <input type="number" min="1" value={rule.catalyst.count} onChange={(event) => onTransformChange(rule.id, { catalystCount: Number(event.target.value) })} />
            </Field>
            <Field label="优先级">
              <input type="number" value={rule.priority} onChange={(event) => onTransformChange(rule.id, { priority: Number(event.target.value) })} />
            </Field>
          </div>
          <div className="field-pair">
            <Field label="材料来源">
              <select value={rule.inputSource} onChange={(event) => onTransformChange(rule.id, { inputSource: event.target.value === "inventory" ? "inventory" : "clicked_hand" })}>
                <option value="clicked_hand">右键手持物品</option>
                <option value="inventory">玩家背包</option>
              </select>
            </Field>
            <Field label="潜行要求">
              <select value={rule.requireSneaking ? "required" : "any"} onChange={(event) => onTransformChange(rule.id, { requireSneaking: event.target.value === "required" })}>
                <option value="any">无需潜行</option>
                <option value="required">必须潜行</option>
              </select>
            </Field>
          </div>
          <div className="field-pair">
            <Field label="触发手">
              <select value={rule.hand} onChange={(event) => onTransformChange(rule.id, { hand: event.target.value as BlockTransform["hand"] })}>
                <option value="main">仅主手</option>
                <option value="off">仅副手</option>
                <option value="any">任意手</option>
              </select>
            </Field>
            <BooleanField
              label="允许 FakePlayer"
              value={rule.allowFakePlayer}
              onChange={(value) => onTransformChange(rule.id, { allowFakePlayer: value })}
            />
          </div>
          <div className="field-pair">
            <BooleanField
              label="生存模式消耗材料"
              value={rule.consumeInput}
              onChange={(value) => onTransformChange(rule.id, { consumeInput: value })}
            />
            <BooleanField
              label="创造模式需要材料"
              value={rule.creativeRequireInput}
              onChange={(value) => onTransformChange(rule.id, { creativeRequireInput: value })}
            />
          </div>
          <BooleanField
            label="创造模式消耗材料"
            value={rule.creativeConsume}
            onChange={(value) => onTransformChange(rule.id, { creativeConsume: value })}
          />
          {rule.inputSource === "inventory" && !rule.requireSneaking && (
            <p className="panel-note">背包取材且无需潜行容易误触；建议设为必须潜行。</p>
          )}
          <RuleValidation
            rule={rule}
            issues={validationIssues.filter((issue) => issue.entityId === rule.id)}
          />
          <KeyValue label="catalyst" value={`${rule.catalyst.ref} ×${rule.catalyst.count}`} />
          <KeyValue label="interaction" value={`${rule.inputSource} / ${rule.hand}`} />
          <KeyValue label="block entity" value={rule.blockEntityPolicy} />
          <button type="button" className="button button--quiet transform-delete" onClick={() => onRemoveTransform(rule.id)}>删除草稿规则</button>
        </section>
      ))}
    </div>
  );
}

function RuleIdField({ rule, siblingIds, onCommit }: {
  rule: BlockTransform;
  siblingIds: string[];
  onCommit: (id: string) => void;
}) {
  const [raw, setRaw] = useState(rule.id);
  useEffect(() => setRaw(rule.id), [rule.id]);
  const error = validateBlockTransformIdInput(raw, rule, siblingIds);
  const commit = () => {
    if (error) {
      setRaw(rule.id);
      return;
    }
    const normalized = normalizeBlockTransformRuleId(raw);
    if (normalized !== rule.id) onCommit(normalized);
    else setRaw(normalized);
  };
  return (
    <Field label="规则 ID（运行配置唯一身份）">
      <input
        className={`mono ${error ? "is-invalid" : ""}`}
        value={raw}
        spellCheck={false}
        onChange={(event) => setRaw(event.target.value)}
        onBlur={commit}
        onKeyDown={(event) => {
          if (event.key === "Enter") event.currentTarget.blur();
          if (event.key === "Escape") {
            setRaw(rule.id);
            event.currentTarget.blur();
          }
        }}
      />
      {error && <small className="field-error" role="alert">{error} 该值尚未写入草稿。</small>}
    </Field>
  );
}

function BlockStateField({ label, value, onChange }: {
  label: string;
  value: BlockTransform["targetState"];
  onChange: (value: BlockTransform["targetState"]) => void;
}) {
  const formatted = useMemo(() => formatBlockStateInput(value), [value]);
  const [raw, setRaw] = useState(formatted);
  const [error, setError] = useState("");
  useEffect(() => {
    if (!error) setRaw(formatted);
  }, [error, formatted]);
  return (
    <Field label={label}>
      <textarea
        className={`mono state-json-input ${error ? "is-invalid" : ""}`}
        rows={3}
        value={raw}
        spellCheck={false}
        onBlur={() => {
          if (!error) return;
          setRaw(formatted);
          setError("");
        }}
        onChange={(event) => {
          const nextRaw = event.target.value;
          setRaw(nextRaw);
          const parsed = parseBlockStateInput(nextRaw);
          if (!parsed.ok) {
            setError(parsed.error);
            return;
          }
          setError("");
          onChange(parsed.value);
        }}
      />
      {error && <small className="field-error" role="alert">{error} 此内容尚未写入草稿，离开输入框会恢复上次有效值。</small>}
    </Field>
  );
}

function CommaListField({ label, value, placeholder, onChange }: {
  label: string;
  value: string[];
  placeholder?: string;
  onChange: (value: string[]) => void;
}) {
  const formatted = value.join(", ");
  const [raw, setRaw] = useState(formatted);
  useEffect(() => setRaw(formatted), [formatted]);
  return (
    <Field label={label}>
      <input
        className="mono"
        value={raw}
        placeholder={placeholder}
        onChange={(event) => {
          setRaw(event.target.value);
          onChange(parseCopyPropertiesInput(event.target.value));
        }}
      />
    </Field>
  );
}

function BooleanField({ label, value, onChange }: { label: string; value: boolean; onChange: (value: boolean) => void }) {
  return (
    <Field label={label}>
      <select value={value ? "yes" : "no"} onChange={(event) => onChange(event.target.value === "yes")}>
        <option value="no">否</option>
        <option value="yes">是</option>
      </select>
    </Field>
  );
}

function RuleValidation({ rule, issues }: { rule: BlockTransform; issues: ValidationIssue[] }) {
  if (!issues.length) {
    return <div className="rule-validation is-clear"><strong>规则结构可导出</strong><span>仍需在测试世界验证实际交互。</span></div>;
  }
  const errors = issues.filter((issue) => issue.severity === "error");
  const blocking = errors.length > 0;
  return (
    <div className={`rule-validation ${blocking ? "is-blocking" : "is-draft"}`} role={blocking ? "alert" : "status"}>
      <strong>{blocking ? "当前规则阻止候选包导出" : rule.enabled ? "规则可导出，但有安全提醒" : "停用草稿或安全提醒"}</strong>
      <ul>
        {issues.map((issue, index) => (
          <li key={`${issue.code}:${issue.message}:${index}`}>{describeBlockTransformIssueZhCn(issue.code, issue.message)}</li>
        ))}
      </ul>
    </div>
  );
}

function RecipeDetails({ recipe, onChange }: {
  recipe: CanonicalRecipe;
  onChange: (id: string, patch: Partial<Pick<CanonicalRecipe, "station" | "stationKey" | "stationScope" | "form" | "level">>) => void;
}) {
  return <>
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
  </>;
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

function transformsFor(project: WorkbenchProject, item: CanonicalItem): BlockTransform[] {
  return project.blockTransforms.filter((entry) => refMatchesItem(entry.catalyst, item));
}

function relevantIssues(project: WorkbenchProject, item: CanonicalItem, recipes: CanonicalRecipe[]) {
  const recipeIds = new Set(recipes.map((recipe) => recipe.id));
  const transformIds = new Set(transformsFor(project, item).map((rule) => rule.id));
  return project.issues.filter((issue) => issue.entityId === item.itemKey
    || (issue.entityType === "recipe" && recipeIds.has(issue.entityId))
    || (issue.entityType === "block_transform" && transformIds.has(issue.entityId))
    || issue.entityId.split("|").includes(item.itemKey));
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
