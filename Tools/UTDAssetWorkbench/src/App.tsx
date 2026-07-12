import { useEffect, useMemo, useRef, useState } from "react";
import { GraphCanvas, type EntitySelection } from "./components/GraphCanvas";
import { Inspector, type InspectorTab } from "./components/Inspector";
import { sampleProject } from "./data/sample";
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
  exportStatusKjs
} from "./domain/exporters";
import type { CanonicalItem, WorkbenchProject } from "./domain/schema";
import { lootMatchesItem, refMatchesItem } from "./domain/relations";
import { catalogIdentityLabel, matchesCatalogQuery } from "./domain/catalogIdentity";

type CatalogFilter = "selected" | "managed" | "dependency" | "issues" | "all";

export default function App() {
  const [project, setProject] = useState<WorkbenchProject>(sampleProject);
  const firstRoot = project.items.find((item) => item.humanSelected)?.itemKey ?? project.items[0]?.itemKey ?? "";
  const [rootItemKey, setRootItemKey] = useState(firstRoot);
  const [selection, setSelection] = useState<EntitySelection>({ kind: "item", id: firstRoot });
  const [tab, setTab] = useState<InspectorTab>("record");
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<CatalogFilter>("selected");
  const [notice, setNotice] = useState("示例档案已载入 · 可导入 CLI 生成的 workbench.json");
  const fileInput = useRef<HTMLInputElement>(null);
  const rootItem = project.items.find((item) => item.itemKey === rootItemKey) ?? project.items.find((item) => item.humanSelected)!;
  const visibleItems = useMemo(() => filterItems(project.items, filter, query), [project.items, filter, query]);
  const rootRecipes = project.recipes.filter((recipe) => recipe.outputs.some((output) => refMatchesItem(output, rootItem)));
  const rootLoot = project.lootPolicies.filter((policy) => lootMatchesItem(policy, rootItem));

  useEffect(() => {
    let cancelled = false;
    fetch("./data/workbench.json", { cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) throw new Error(String(response.status));
        const parsed: unknown = await response.json();
        assertWorkbenchProject(parsed);
        if (cancelled) return;
        const nextRoot = parsed.items.find((item) => item.humanSelected)?.itemKey;
        if (!nextRoot) return;
        setProject(parsed);
        setRootItemKey(nextRoot);
        setSelection({ kind: "item", id: nextRoot });
        setNotice(`已自动载入 public/data/workbench.json · ${parsed.items.length} 个状态身份`);
      })
      .catch(() => undefined);
    return () => { cancelled = true; };
  }, []);

  const chooseItem = (item: CanonicalItem) => {
    if (item.humanSelected) setRootItemKey(item.itemKey);
    setSelection({ kind: "item", id: item.itemKey });
    setTab("record");
  };

  const chooseGraphEntity = (next: EntitySelection) => {
    setSelection(next);
    if (next.kind === "recipe") setTab("recipe");
    else setTab("record");
  };

  const importProject = async (file: File) => {
    try {
      const parsed: unknown = JSON.parse(await file.text());
      assertWorkbenchProject(parsed);
      const nextRoot = parsed.items.find((item) => item.humanSelected)?.itemKey;
      if (!nextRoot) throw new Error("Project has no human_selected whitelist root.");
      setProject(parsed);
      setRootItemKey(nextRoot);
      setSelection({ kind: "item", id: nextRoot });
      setFilter("selected");
      setTab("record");
      setNotice(`已载入 ${file.name} · ${parsed.manifest.counts.managedItems} 个纳管身份`);
    } catch (error) {
      setNotice(`载入失败：${error instanceof Error ? error.message : String(error)}`);
    }
  };

  const save = (kind: "project" | "recipes" | "loot" | "status" | "statusKjs" | "excel" | "presentation" | "lang" | "transforms") => {
    const base = project.manifest.projectId.replace(/[^a-zA-Z0-9_-]+/g, "_");
    const exports = {
      project: [`${base}.workbench.json`, exportProjectJson(project), "application/json"],
      recipes: ["utd_recipe_data.filtered.js", exportRecipeKjs(project), "text/javascript"],
      loot: ["utd_loot_registry_data.filtered.js", exportLootRegistryKjs(project), "text/javascript"],
      status: ["status_manifest.json", exportStatusJson(project), "application/json"],
      statusKjs: ["utd_asset_status_manifest.js", exportStatusKjs(project), "text/javascript"],
      excel: ["utd_asset_excel_interface.json", exportExcelInterfaceJson(project), "application/json"],
      presentation: ["utd_item_presentations.json", exportPresentationJson(project), "application/json"],
      lang: ["utd_lang_overlays.json", exportLangOverlaysJson(project), "application/json"],
      transforms: ["utd_block_transforms.json", exportBlockTransformsJson(project), "application/json"]
    } as const;
    const [filename, text, mime] = exports[kind];
    download(filename, text, mime);
    setNotice(`已生成 ${filename}`);
  };

  return (
    <div className="workbench-shell">
      <header className="topbar">
        <div className="brand-block">
          <div className="brand-mark"><span>U</span><i /></div>
          <div>
            <p>誓死坚守 · RESOURCE CONTROL</p>
            <h1>ASSET WORKBENCH <small>01</small></h1>
          </div>
        </div>
        <div className="topbar-status">
          <Metric label="CATALOG" value={project.items.length} />
          <Metric label="RECIPES" value={project.recipes.length} />
          <Metric label="LOOT" value={project.lootPolicies.length} />
          <Metric label="ISSUES" value={project.manifest.counts.issues} alert={project.manifest.counts.issues > 0} />
        </div>
        <div className="topbar-actions">
          <input
            ref={fileInput}
            type="file"
            accept="application/json,.json"
            hidden
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) void importProject(file);
              event.target.value = "";
            }}
          />
          <button type="button" className="button button--quiet" onClick={() => fileInput.current?.click()}>载入 JSON</button>
          <details className="export-menu">
            <summary className="button button--primary">生成文件</summary>
            <div>
              <button type="button" onClick={() => save("project")}><span>规范项目</span><small>workbench.json</small></button>
              <button type="button" onClick={() => save("recipes")}><span>配方 KJS</span><small>完整载荷</small></button>
              <button type="button" onClick={() => save("loot")}><span>Loot KJS</span><small>完整载荷</small></button>
              <button type="button" onClick={() => save("status")}><span>状态 JSON</span><small>Tooltip / UI</small></button>
              <button type="button" onClick={() => save("statusKjs")}><span>状态 KJS</span><small>global.UTD</small></button>
              <button type="button" onClick={() => save("presentation")}><span>物品显示覆盖</span><small>独立草稿 JSON</small></button>
              <button type="button" onClick={() => save("lang")}><span>中文语言覆盖</span><small>按 namespace 分组</small></button>
              <button type="button" onClick={() => save("transforms")}><span>方块替换规则</span><small>独立草稿 JSON</small></button>
              <button type="button" onClick={() => save("excel")}><span>Excel 接口</span><small>单向审阅 JSON</small></button>
            </div>
          </details>
        </div>
      </header>

      <main className="workbench-grid">
        <nav className="catalog-rail" aria-label="资产目录">
          <div className="rail-heading">
            <span>01 / ASSET INDEX</span>
            <strong>物品目录</strong>
          </div>
          <label className="search-box">
            <span>⌕</span>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="名称 / ID / 变体" />
            {query && <button type="button" onClick={() => setQuery("")}>×</button>}
          </label>
          <div className="catalog-filters">
            <FilterButton active={filter === "selected"} onClick={() => setFilter("selected")}>白名单</FilterButton>
            <FilterButton active={filter === "managed"} onClick={() => setFilter("managed")}>纳管</FilterButton>
            <FilterButton active={filter === "dependency"} onClick={() => setFilter("dependency")}>依赖</FilterButton>
            <FilterButton active={filter === "issues"} onClick={() => setFilter("issues")}>问题</FilterButton>
          </div>
          <div className="catalog-count"><span>{visibleItems.length}</span> / {project.items.length} RECORDS</div>
          <div className="catalog-list">
            {visibleItems.map((item, index) => {
              const identity = catalogIdentityLabel(item);
              return (
                <button
                  type="button"
                  key={item.itemKey}
                  className={`${item.itemKey === rootItemKey ? "is-root" : ""} ${selection.kind === "item" && selection.id === item.itemKey ? "is-selected" : ""} ${item.managed ? "is-managed" : "is-external"}`}
                  onClick={() => chooseItem(item)}
                  style={{ animationDelay: `${Math.min(index * 15, 240)}ms` }}
                  aria-label={`${item.clientNameZhCn}，${item.registryId}${identity ? `，${identity.exact}` : ""}`}
                >
                  <span className="catalog-item__rail" />
                  <span className="catalog-item__body">
                    <strong title={item.clientNameZhCn}>{item.clientNameZhCn}</strong>
                    <small title={item.registryId}>{item.registryId}</small>
                    {identity && (
                      <small className="catalog-item__identity" title={`${identity.exact}\nassetKey=${item.itemKey}`}>
                        <b>{identity.primary}</b>
                        <em>{identity.context}</em>
                      </small>
                    )}
                  </span>
                  <span className="catalog-item__status">
                    {item.humanSelected && <i title="Human selected">H</i>}
                    {item.lootEnabled && <i title={`Loot level ${item.lootLevel ?? "?"}`}>L{item.lootLevel ?? ""}</i>}
                    {item.issues.length > 0 && <i className="is-issue">!</i>}
                  </span>
                </button>
              );
            })}
            {!visibleItems.length && <div className="catalog-empty">没有匹配记录</div>}
          </div>
          <div className="rail-footer">
            <span className={`sync-dot sync-dot--${project.items.some((item) => item.sync === "pending") ? "pending" : "local_only"}`} />
            <div><strong>{project.manifest.projectId}</strong><small>{project.manifest.contentFingerprint}</small></div>
          </div>
        </nav>

        <section className="graph-workspace">
          <div className="workspace-heading">
            <div>
              <span>02 / ONE-HOP RECIPE GRAPH</span>
              <h2>{rootItem.clientNameZhCn}</h2>
              <p className="mono">{rootItem.itemKey}</p>
            </div>
            <div className="workspace-summary">
              <span><i className="legend legend--root" />白名单根</span>
              <span><i className="legend legend--recipe" />配方</span>
              <span><i className="legend legend--external" />外部叶</span>
              <span><i className="legend legend--tag" />Tag 终点</span>
            </div>
            <div className="workspace-stamp">
              <strong>{String(rootRecipes.length).padStart(2, "0")}</strong>
              <span>RECIPES</span>
              <small>{rootLoot.length} LOOT</small>
            </div>
          </div>
          <div className="scope-strip">
            <span>SCOPE LOCK</span>
            <strong>仅白名单根 + 直接依赖叶</strong>
            <i />
            <span>TAG POLICY</span>
            <strong>保留引用，不展开</strong>
          </div>
          <GraphCanvas project={project} rootItemKey={rootItemKey} selection={selection} onSelect={chooseGraphEntity} />
          <div className="workspace-notice" role="status">{notice}</div>
        </section>

        <Inspector
          project={project}
          rootItemKey={rootItemKey}
          selection={selection}
          tab={tab}
          onTab={setTab}
          onSelection={setSelection}
          setProject={setProject}
        />
      </main>
    </div>
  );
}

function Metric({ label, value, alert = false }: { label: string; value: number; alert?: boolean }) {
  return <div className={alert ? "is-alert" : ""}><span>{label}</span><strong>{String(value).padStart(2, "0")}</strong></div>;
}

function FilterButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return <button type="button" className={active ? "is-active" : ""} onClick={onClick}>{children}</button>;
}

function filterItems(items: CanonicalItem[], filter: CatalogFilter, query: string): CanonicalItem[] {
  return items
    .filter((item) => {
      if (filter === "selected") return item.humanSelected;
      if (filter === "managed") return item.managed;
      if (filter === "dependency") return !item.managed;
      if (filter === "issues") return item.issues.length > 0;
      return true;
    })
    .filter((item) => matchesCatalogQuery(item, query))
    .sort((a, b) => Number(b.humanSelected) - Number(a.humanSelected)
      || Number(b.managed) - Number(a.managed)
      || a.clientNameZhCn.localeCompare(b.clientNameZhCn, "zh-CN")
      || a.variantDiscriminator.localeCompare(b.variantDiscriminator)
      || a.itemKey.localeCompare(b.itemKey));
}

function download(filename: string, text: string, mime: string): void {
  const url = URL.createObjectURL(new Blob([text], { type: `${mime};charset=utf-8` }));
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  window.setTimeout(() => URL.revokeObjectURL(url), 0);
}
