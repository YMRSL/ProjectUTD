import { buildWorkbenchProject } from "../domain/build";

export const sampleProject = buildWorkbenchProject({
  projectId: "utd-assets-demo",
  generatedAt: "2026-07-11T00:00:00.000Z",
  snapshot: {
    schemaVersion: "utd-item-whitelist/v1",
    items: [
      {
        registry_id: "zombiekit:baseball_bat",
        client_name_zh_cn: "实木棒球棍",
        translation_key: "item.zombiekit.baseball_bat",
        canonical_components: {}
      },
      {
        registry_id: "create:shaft",
        client_name_zh_cn: "传动杆",
        translation_key: "item.create.shaft",
        canonical_components: {}
      },
      {
        registry_id: "firstpersonfoodeating:pack_food",
        client_name_zh_cn: "高能量棒",
        translation_key: "item.firstpersonfoodeating.pack_food",
        variant_key: "energy_bar_a",
        canonical_variant: { food_id: "firstpersonfoodeating:i_bang_a" },
        canonical_components: {
          "firstpersonfoodeating:profile": { food_id: "firstpersonfoodeating:i_bang_a" }
        }
      },
      {
        registry_id: "utd_catalog:unassigned_sample",
        client_name_zh_cn: "待归档样本",
        translation_key: "",
        canonical_components: {}
      }
    ]
  },
  recipeData: {
    generatedAt: "2026-07-11T00:00:00.000Z",
    removeTypes: [],
    removeRecipeIds: [],
    removeOutputs: ["zombiekit:baseball_bat"],
    shaped: [
      {
        id: "kubejs:utd_crafting/melee/baseball_bat",
        pattern: ["A ", "A ", " B"],
        key: {
          A: { item: "minecraft:oak_slab" },
          B: { item: "zombiekit:iron_wire" }
        },
        output: { id: "zombiekit:baseball_bat", count: 1 },
        utd: {
          sheet: "melee",
          row: 7,
          station: "徒手 / 书桌",
          stationKey: "crafting",
          stationScope: "desk_crafting",
          form: "有序",
          outputName: "实木棒球棍",
          output: "zombiekit:baseball_bat",
          level: 0,
          count: 1,
          outputKeys: ["zombiekit:baseball_bat"]
        }
      }
    ],
    shapeless: [],
    custom: [
      {
        id: "kubejs:utd_processing/components/shaft",
        json: {
          type: "create:cutting",
          ingredients: [{ tag: "kubejs:utd_crafting/shaft_stock" }],
          results: [{ item: "create:shaft", count: 2 }],
          processingTime: 200
        },
        utd: {
          sheet: "components",
          row: 5,
          station: "动力锯",
          stationKey: "processing",
          stationScope: "processing",
          form: "切削",
          outputName: "传动杆",
          output: "create:shaft",
          outputKeys: ["create:shaft"],
          level: 1,
          count: 2
        }
      }
    ]
  },
  lootRegistry: [
    {
      id: "firstpersonfoodeating:pack_food{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_a\"}}",
      lootItemId: "firstpersonfoodeating:pack_food",
      lootEnabled: true,
      level: 1,
      count: 1,
      commonTags: ["food_packaged", "polymer_container"],
      commonBaseWeight: 3,
      allowedCommonTemplates: ["food_service", "retail_home"],
      directedWeight: 0,
      replacePriority: 1
    }
  ]
});
