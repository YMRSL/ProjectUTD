package net.tkg.ModernMayhem.server.config;

import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.server.util.ArmorProperties;
import net.tkg.ModernMayhem.server.util.BackpackStorageProperties;
import net.tkg.ModernMayhem.server.util.CuriosBodyProperties;
import net.tkg.ModernMayhem.server.util.CuriosFacewearProperties;
import net.tkg.ModernMayhem.server.util.RigStorageProperties;

public class ArmorConfig {
    public static void init() {
        for (ArmorProperties armorProperties : ArmorProperties.values()) {
            ModernMayhemMod.getModContainer().registerConfig(ModConfig.Type.SERVER, (IConfigSpec)armorProperties.getConfig(), "modern-mayhem/armor-config/" + armorProperties.getName() + ".toml");
        }
        for (Enum enum_ : CuriosBodyProperties.values()) {
            ModernMayhemMod.getModContainer().registerConfig(ModConfig.Type.SERVER, (IConfigSpec)((CuriosBodyProperties)enum_).getConfig(), "modern-mayhem/curios-config/body/" + ((CuriosBodyProperties)enum_).getName() + ".toml");
        }
        for (Enum enum_ : CuriosFacewearProperties.values()) {
            ModernMayhemMod.getModContainer().registerConfig(ModConfig.Type.SERVER, (IConfigSpec)((CuriosFacewearProperties)enum_).getConfig(), "modern-mayhem/curios-config/facewear/" + ((CuriosFacewearProperties)enum_).getName() + ".toml");
        }
        for (Enum enum_ : BackpackStorageProperties.values()) {
            ModernMayhemMod.getModContainer().registerConfig(ModConfig.Type.SERVER, (IConfigSpec)((BackpackStorageProperties)enum_).getConfig(), "modern-mayhem/curios-config/backpack/" + ((BackpackStorageProperties)enum_).getName() + ".toml");
        }
        for (Enum enum_ : RigStorageProperties.values()) {
            ModernMayhemMod.getModContainer().registerConfig(ModConfig.Type.SERVER, (IConfigSpec)((RigStorageProperties)enum_).getConfig(), "modern-mayhem/curios-config/body/storage/" + ((RigStorageProperties)enum_).getName() + ".toml");
        }
    }
}

