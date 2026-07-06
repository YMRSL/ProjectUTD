package com.scarasol.tud.init;

import com.scarasol.tud.api.functional.AmmoGetter;
import com.scarasol.tud.api.functional.EntityGetter;
import com.scarasol.tud.api.functional.ModifierGetter;
import com.scarasol.tud.data.*;
import com.scarasol.tud.manager.AmmoManager;
import com.scarasol.tud.manager.ModifierManager;
import com.scarasol.tud.util.data.DataManager;
import com.scarasol.tud.util.io.ModGson;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import net.minecraft.resources.ResourceLocation;
// 1.20->1.21: ResourceLocation.fromNamespaceAndPath(ns,path) -> ResourceLocation.fromNamespaceAndPath(ns,path)

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Scarasol
 */
public class TudModData {

    public static void registerType() {
        ModGson.INSTANCE.register(AmmoData.class);
        ModGson.INSTANCE.register(GunData.class);
        ModGson.INSTANCE.register(ModifierData.class);
        DataManager.registerSearchableType(AmmoData.class, 1, true);
        DataManager.registerSearchableType(GunData.class, 2, true);
        DataManager.registerType(AmmoGetter.class, 3, false);
        DataManager.registerType(EntityGetter.class, 4, true);
        DataManager.registerSearchableType(TaczGunDataMap.class, 5, true);
        DataManager.registerSearchableType(ModifierData.class, 6, true);
        DataManager.registerType(ModifierGetter.class, 7, true);
    }

    public static void registerAmmoGetter() {
        DataManager.registerModData(AmmoGetter.class, AmmoManager::getCurrentAmmoFromToml);
        DataManager.registerModData(AmmoGetter.class, AmmoManager::getCurrentAmmoFromJson);
    }

    public static void registerModifierGetter() {
        DataManager.registerModData(ModifierGetter.class, ModifierManager::getModifierById);
        DataManager.registerModData(ModifierGetter.class, ModifierManager::getModifierByTag);
        DataManager.registerModData(ModifierGetter.class, ModifierManager::getModifierByMobType);
    }

    public static void initModData(Path root) throws IOException {
        Files.createDirectories(root);

        // 1) GunData
        GunData exampleGunData = makeExampleGunData();
        ModGson.INSTANCE.write(exampleGunData.getPath(), exampleGunData);

        // 2) AmmoData - required example
        AmmoData requiredExampleAmmoData = makeRequiredExampleAmmoData();
        ModGson.INSTANCE.write(requiredExampleAmmoData.getPath(), requiredExampleAmmoData);

        // 3) AmmoData - full example
        AmmoData fullExampleAmmoData = makeFullExampleAmmoData();
        ModGson.INSTANCE.write(fullExampleAmmoData.getPath(), fullExampleAmmoData);

        // 4) ModifierData - example_modifier
        ModifierData exampleModifier = makeExampleModifier();
        ModGson.INSTANCE.write(exampleModifier.getPath(), exampleModifier);
    }

    public static GunData makeExampleGunData() {
        return new GunData(
                ResourceLocation.fromNamespaceAndPath("tacz", "example_gun"),
                List.of(
                        new MagData(
                                ResourceLocation.fromNamespaceAndPath("tacz", "required_example_ammo"),
                                null,
                                null,
                                null
                        ),
                        new MagData(
                                ResourceLocation.fromNamespaceAndPath("tacz", "full_example_ammo"),
                                30,
                                600,
                                new Integer[]{45, 60, 75}
                        )
                )
        );
    }

    public static AmmoData makeRequiredExampleAmmoData() {

        return new AmmoData(ResourceLocation.fromNamespaceAndPath("tacz", "required_example_ammo"), false);
    }

    public static AmmoData makeFullExampleAmmoData() {
        AmmoData data = new AmmoData();
        data.setAmmoId(ResourceLocation.fromNamespaceAndPath("tacz", "full_example_ammo"));


        data.setIsItem(false);

        data.setEntityId(ResourceLocation.fromNamespaceAndPath("minecraft", "arrow"));


        data.setSpeed(160f);
        data.setGravity(0.25f);
        data.setFriction(0.035f);


        data.setDamageAmount(9.5f);
        data.setKnockback(0.15f);


        data.setExplosion(true);
        data.setExplosionDamage(12.0f);
        data.setExplosionRadius(3.5f);
        data.setExplosionDelayCount(30);
        data.setExplosionKnockback(true);
        data.setExplosionDestroyBlock(false);

        data.setIgniteEntity(true);
        data.setIgniteEntityTime(4);
        data.setIgniteBlock(true);


        data.setPierce(2);
        data.setTracerAmmo(true);


        data.setArmorIgnore(0.25f);
        data.setHeadShot(1.75f);


        data.setEffectDataList(List.of(
                new EffectData(ResourceLocation.fromNamespaceAndPath("minecraft", "speed"), 0, 5),
                new EffectData(ResourceLocation.fromNamespaceAndPath("minecraft", "weakness"), 1, 2)
        ));


        LinkedList<ExtraDamage.DistanceDamagePair> adjust = new LinkedList<>();
        adjust.add(new ExtraDamage.DistanceDamagePair(40.0f, 9.5f));
        adjust.add(new ExtraDamage.DistanceDamagePair(15.0f, 8.0f));
        adjust.add(new ExtraDamage.DistanceDamagePair(30.0f, 6.5f));
        adjust.add(new ExtraDamage.DistanceDamagePair(Float.MAX_VALUE, 5.0f));
        data.setDamageAdjust(adjust);


        data.setModifierId("tud:example_modifier");

        return data;
    }

    public static ModifierData makeExampleModifier() {
        return new ModifierData(
                ResourceLocation.fromNamespaceAndPath("tud", "example_modifier"),
                Map.of(
                        "minecraft:zombie", 1.20D,
                        "minecraft:freeze_immune_entity_types", 0.85D,
                        "undead", 1.10D
                )
        );
    }

}
