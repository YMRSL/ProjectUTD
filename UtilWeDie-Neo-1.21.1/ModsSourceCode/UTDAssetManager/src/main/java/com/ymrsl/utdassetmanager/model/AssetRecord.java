package com.ymrsl.utdassetmanager.model;

public final class AssetRecord {
    public String assetKey = "";
    public String variantKey = "";
    public String registryId = "";
    public String modId = "";
    public String variantKind = "plain";
    public String variantDiscriminator = "";
    public String componentsSnbt = "{}";
    public String componentsCanonical = "{}";
    public String identityComponentsCanonical = "{}";
    public String itemStackSnbt = "{}";
    public String translationKey = "";
    public String displayNameZhCn = "";
    public String capturedLocale = "";
    public boolean humanSelected = true;
    public String selectedAt = "";
    public String updatedAt = "";

    public AssetRecord copy() {
        AssetRecord copy = new AssetRecord();
        copy.assetKey = assetKey;
        copy.variantKey = variantKey;
        copy.registryId = registryId;
        copy.modId = modId;
        copy.variantKind = variantKind;
        copy.variantDiscriminator = variantDiscriminator;
        copy.componentsSnbt = componentsSnbt;
        copy.componentsCanonical = componentsCanonical;
        copy.identityComponentsCanonical = identityComponentsCanonical;
        copy.itemStackSnbt = itemStackSnbt;
        copy.translationKey = translationKey;
        copy.displayNameZhCn = displayNameZhCn;
        copy.capturedLocale = capturedLocale;
        copy.humanSelected = humanSelected;
        copy.selectedAt = selectedAt;
        copy.updatedAt = updatedAt;
        return copy;
    }
}
