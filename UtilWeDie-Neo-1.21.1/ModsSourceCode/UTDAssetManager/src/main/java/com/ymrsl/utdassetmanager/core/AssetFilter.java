package com.ymrsl.utdassetmanager.core;

public enum AssetFilter {
    ALL("全部"),
    HUMAN_SELECTED("人工标注"),
    CATALOGUED("已纳管"),
    RECIPE("配方关联"),
    LOOT("Loot 启用"),
    UNSYNCED("待同步"),
    ISSUES("有异常");

    private final String label;

    AssetFilter(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean matches(AssetStatus status) {
        return switch (this) {
            case ALL -> true;
            case HUMAN_SELECTED -> status.humanSelected();
            case CATALOGUED -> status.catalogued();
            case RECIPE -> status.hasRecipe();
            case LOOT -> status.lootEnabled();
            case UNSYNCED -> status.needsSync();
            case ISSUES -> !status.issues().isEmpty();
        };
    }
}
