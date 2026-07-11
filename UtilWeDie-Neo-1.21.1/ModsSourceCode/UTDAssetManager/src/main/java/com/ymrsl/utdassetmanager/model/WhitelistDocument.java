package com.ymrsl.utdassetmanager.model;

import java.util.ArrayList;
import java.util.List;

public final class WhitelistDocument {
    public int schemaVersion = 1;
    public String updatedAt = "";
    public String producer = "utd_asset_manager";
    public List<AssetRecord> entries = new ArrayList<>();
}
