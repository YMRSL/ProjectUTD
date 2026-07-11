package com.ymrsl.utdassetmanager.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class AssetIdentityTest {
    @Test
    void sameRegistryAndCanonicalComponentsProduceStableIdentity() {
        String first = AssetIdentity.assetKey("TaCZ:Modern_Kinetic_Gun", "C{5:GunId=T8:4:\"ak\";}");
        String second = AssetIdentity.assetKey("tacz:modern_kinetic_gun", "C{5:GunId=T8:4:\"ak\";}");
        assertEquals(first, second);
    }

    @Test
    void componentVariantChangesIdentity() {
        String ak = AssetIdentity.assetKey("tacz:modern_kinetic_gun", "C{GunId=ak}");
        String m4 = AssetIdentity.assetKey("tacz:modern_kinetic_gun", "C{GunId=m4}");
        assertNotEquals(ak, m4);
    }

    @Test
    void variantKindRecognizesTaczAndFpeComponents() {
        assertEquals("tacz_component", AssetIdentity.variantKind("tacz:modern_kinetic_gun", "{GunId:\"x\"}"));
        assertEquals("fpe_component", AssetIdentity.variantKind("firstpersonfoodeating:pack_food",
                "{minecraft:custom_data:{firstpersonfoodeating_profile:{food_id:\"x\"}}}"));
    }

    @Test
    void variantDiscriminatorUsesTheFrozenTaczAndFpeKeys() {
        assertEquals("GunId=tacz:ak47", AssetIdentity.variantDiscriminator(
                "{minecraft:custom_data:{GunId:\"tacz:ak47\",AmmoId:\"tacz:762\"}}"));
        assertEquals("AmmoId=tacz:762", AssetIdentity.variantDiscriminator(
                "{minecraft:custom_data:{AmmoId:\"tacz:762\"}}"));
        assertEquals("AttachmentId=tacz:scope", AssetIdentity.variantDiscriminator(
                "{minecraft:custom_data:{AttachmentId:\"tacz:scope\"}}"));
        assertEquals("food_id=firstpersonfoodeating:i_bang_a", AssetIdentity.variantDiscriminator(
                "{minecraft:custom_data:{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_a\"}}}"));
        assertEquals("", AssetIdentity.variantDiscriminator("{}"));
    }

    @Test
    void runtimeTaczFieldsDoNotChangeTheLogicalAssetIdentity() {
        String discriminator = "GunId=tacz:ak47";
        String loaded = AssetIdentity.identityComponentsCanonical(
                "C{GunCurrentAmmoCount=30;GunFireMode=SEMI;GunId=tacz:ak47;}", discriminator);
        String fired = AssetIdentity.identityComponentsCanonical(
                "C{GunCurrentAmmoCount=12;GunFireMode=AUTO;GunId=tacz:ak47;}", discriminator);
        assertEquals(loaded, fired);
        assertEquals(
                AssetIdentity.assetKey("tacz:modern_kinetic_gun", loaded),
                AssetIdentity.assetKey("tacz:modern_kinetic_gun", fired));
    }

    @Test
    void differentLogicalVariantDiscriminatorsRemainDistinct() {
        String ak = AssetIdentity.identityComponentsCanonical("ignored", "GunId=tacz:ak47");
        String m4 = AssetIdentity.identityComponentsCanonical("ignored", "GunId=tacz:m4a1");
        String foodA = AssetIdentity.identityComponentsCanonical("ignored", "food_id=firstpersonfoodeating:i_bang_a");
        String foodB = AssetIdentity.identityComponentsCanonical("ignored", "food_id=firstpersonfoodeating:i_bang_b");
        assertNotEquals(ak, m4);
        assertNotEquals(foodA, foodB);
    }

    @Test
    void invalidRegistryIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AssetIdentity.assetKey("missing_namespace", "{}"));
    }
}
