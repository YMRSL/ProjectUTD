package com.ymrsl.utdassetmanager.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public final class ManifestEntry {
    @SerializedName(value = "asset_key", alternate = {"assetKey", "item_key", "itemKey"})
    public String assetKey = "";

    @SerializedName(value = "registry_id", alternate = {"registryId"})
    public String registryId = "";

    @SerializedName(value = "variant_key", alternate = {"variantKey"})
    public String variantKey = "";

    @SerializedName(value = "identity_kind", alternate = {"identityKind", "variant_kind", "variantKind"})
    public String identityKind = "plain";

    @SerializedName(value = "variant_discriminator", alternate = {"variantDiscriminator"})
    public String variantDiscriminator = "";

    public boolean catalogued;

    @SerializedName(value = "recipe_input_count", alternate = {"recipeInputCount"})
    public int recipeInputCount;

    @SerializedName(value = "recipe_output_count", alternate = {"recipeOutputCount"})
    public int recipeOutputCount;

    @SerializedName(value = "loot_enabled", alternate = {"lootEnabled"})
    public boolean lootEnabled;

    @SerializedName(value = "loot_level", alternate = {"lootLevel"})
    public int lootLevel;

    @SerializedName(value = "sync_state", alternate = {"syncState", "sync"})
    public String syncState = "local_only";

    @SerializedName(value = "catalog_hash", alternate = {"catalogHash"})
    public String catalogHash = "";

    @SerializedName(value = "deployed_hash", alternate = {"deployedHash"})
    public String deployedHash = "";

    public boolean stale;
    public List<String> issues = new ArrayList<>();
}
