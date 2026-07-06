package com.scarasol.zombiekit.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * 伪装贴图/模型在 1.21 由客户端代理按 getCamouflage() 选择。
 * 贴图：riot_suit_standard / _desert / _forest / _snow。
 */
public class RiotArmor extends CamouflageArmor {

    public RiotArmor(Holder<ArmorMaterial> armorMaterial, Type equipmentSlot, Properties properties, int camouflage) {
        super(armorMaterial, equipmentSlot, properties, camouflage);
    }
}
