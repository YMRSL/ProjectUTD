package com.ymrsl.utdassetmanager.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public final class StatusManifest {
    @SerializedName(value = "schema_version", alternate = {"schemaVersion"})
    public String schemaVersion = "1";

    @SerializedName(value = "generated_at", alternate = {"generatedAt"})
    public String generatedAt = "";

    @SerializedName(value = "source_revision", alternate = {"sourceRevision", "content_fingerprint"})
    public String sourceRevision = "";

    @SerializedName(value = "items", alternate = {"entries"})
    public List<ManifestEntry> entries = new ArrayList<>();
}
