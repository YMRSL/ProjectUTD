package com.projectutd.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LootIdentityTest {
    @Test
    void resolvesTaczGunIdLikeTheRetiredKubeJsScanner() {
        assertEquals("tacz:aa12", LootIdentity.resolve(
                "tacz:modern_kinetic_gun",
                "{GunCurrentAmmoCount:8,GunFireMode:\"SEMI\",GunId:\"tacz:aa12\"}"));
    }

    @Test
    void resolvesFpeFoodIdLikeTheRetiredKubeJsScanner() {
        assertEquals(
                "firstpersonfoodeating:pack_food{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_a\"}}",
                LootIdentity.resolve("firstpersonfoodeating:pack_food",
                        "{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_a\"}}"));
    }

    @Test
    void resolvesTaczWorkbenchVariantAndKeepsPlainItemsStable() {
        assertEquals("tacz:workbench_b{BlockId:\"tacz:ammo_workbench\"}", LootIdentity.resolve(
                "tacz:workbench_b", "{BlockId:\"tacz:ammo_workbench\"}"));
        assertEquals("minecraft:diamond", LootIdentity.resolve("minecraft:diamond", "{}"));
    }
}
