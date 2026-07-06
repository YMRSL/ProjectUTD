package com.scarasol.zombiekit.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

/**
 * 伪装贴图/模型在 1.21 由客户端代理（RegisterClientExtensionsEvent + 自定义
 * HumanoidModel）按 getCamouflage() 选择，故本类只保留伪装等级与材料。
 * 标准/沙漠/森林/雪地贴图：tactical_suit_standard / _desert / _forest / _snow。
 */
public class TacticalArmor extends CamouflageArmor {

    public TacticalArmor(Holder<ArmorMaterial> armorMaterial, Type equipmentSlot, Properties properties, int camouflage) {
        super(armorMaterial, equipmentSlot, properties, camouflage);
    }
}
