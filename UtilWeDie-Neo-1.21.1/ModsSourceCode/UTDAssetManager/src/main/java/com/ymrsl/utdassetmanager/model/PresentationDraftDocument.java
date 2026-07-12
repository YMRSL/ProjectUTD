package com.ymrsl.utdassetmanager.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** On-disk contract for presentation_drafts.json. */
public final class PresentationDraftDocument {
    public static final String SCHEMA = "utd-item-presentation/v1";

    @SerializedName("schema_version")
    public String schemaVersion = SCHEMA;

    public String producer = "utd_asset_manager";

    @SerializedName("updated_at")
    public String updatedAt = "";

    public List<PresentationDraft> drafts = new ArrayList<>();
}
