package com.ymrsl.utdassetmanager.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * A non-destructive editing intent for an item's Chinese presentation.
 *
 * <p>This model deliberately contains no ItemStack component payload. Applying names or descriptions to
 * {@code custom_name}/{@code lore} would change component-sensitive identities, so deployment remains the job of the
 * external content generator.</p>
 */
public final class PresentationDraft {
    @SerializedName("asset_key")
    public String assetKey = "";

    @SerializedName("registry_id")
    public String registryId = "";

    @SerializedName("variant_discriminator")
    public String variantDiscriminator = "";

    @SerializedName("apply_scope")
    public PresentationApplyScope applyScope = PresentationApplyScope.IDENTITY;

    @SerializedName("observed_name_zh_cn")
    public String observedNameZhCn = "";

    @SerializedName("name_zh_cn")
    public String nameZhCn = "";

    @SerializedName("description_zh_cn")
    public List<String> descriptionZhCn = new ArrayList<>();

    public boolean enabled = true;

    @SerializedName("base_catalog_hash")
    public String baseCatalogHash = "";

    @SerializedName("updated_at")
    public String updatedAt = "";

    public PresentationDraft copy() {
        PresentationDraft copy = new PresentationDraft();
        copy.assetKey = assetKey;
        copy.registryId = registryId;
        copy.variantDiscriminator = variantDiscriminator;
        copy.applyScope = applyScope;
        copy.observedNameZhCn = observedNameZhCn;
        copy.nameZhCn = nameZhCn;
        copy.descriptionZhCn = descriptionZhCn == null ? null : new ArrayList<>(descriptionZhCn);
        copy.enabled = enabled;
        copy.baseCatalogHash = baseCatalogHash;
        copy.updatedAt = updatedAt;
        return copy;
    }
}
