// priority: 60

global.UTD = global.UTD || {};

(function () {
  var utd = global.UTD;
  utd.recipeData = {
    "custom": [],
    "generatedAt": "2026-07-11T00:00:00Z",
    "removeOutputs": [
      "zombiekit:baseball_bat"
    ],
    "removeRecipeIds": [],
    "removeTypes": [],
    "shaped": [
      {
        "id": "kubejs:utd_sample/baseball_bat",
        "key": {
          "A": {
            "item": "minecraft:oak_slab"
          },
          "B": {
            "item": "zombiekit:iron_wire"
          }
        },
        "output": {
          "count": 1,
          "id": "zombiekit:baseball_bat"
        },
        "pattern": [
          "A ",
          "A ",
          " B"
        ],
        "utd": {
          "count": 1,
          "form": "有序",
          "level": 0,
          "output": "zombiekit:baseball_bat",
          "outputKeys": [
            "zombiekit:baseball_bat"
          ],
          "outputName": "实木棒球棍",
          "row": 1,
          "sheet": "sample",
          "station": "徒手 / 书桌",
          "stationKey": "crafting",
          "stationScope": "desk_crafting"
        }
      }
    ],
    "shapeless": []
  };
})();
