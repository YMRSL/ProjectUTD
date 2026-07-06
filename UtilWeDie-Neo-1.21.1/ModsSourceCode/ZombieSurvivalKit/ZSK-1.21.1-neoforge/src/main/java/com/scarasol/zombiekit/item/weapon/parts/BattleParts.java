package com.scarasol.zombiekit.item.weapon.parts;

import com.scarasol.sona.util.SonaLottery;
import com.scarasol.zombiekit.item.api.Parts;
import com.scarasol.zombiekit.item.api.SingleHandWeapon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

public abstract class BattleParts extends Item implements Parts {

    private final int partsLevel;

    private final SonaLottery lottery;

    public BattleParts(Properties properties, int partsLevel) {
        super(properties);
        this.partsLevel = partsLevel;
        lottery = new SonaLottery(100, (int) (getPercentage() * 100));
    }

    public abstract void partsEffect(LivingEntity attacker, LivingEntity target, float damage) ;

    @Override
    public int getPartsLevel() {
        return this.partsLevel;
    }

    @Override
    public PartsType getPartsType() {
        return PartsType.BATTLE;
    }

    public double getPercentage() {
        if (this.partsLevel == 0)
            return 0.2;
        else if (this.partsLevel == 1)
            return 0.35;
        else
            return 0.5;
    }

    public static boolean unlock(ItemStack itemStack) {
        return itemStack.getItem() instanceof TieredItem tieredItem && (itemStack.getItem() instanceof SingleHandWeapon || tieredItem.getTier() == Tiers.NETHERITE);
    }

    @Override
    public boolean canUse(ItemStack itemStack) {
        if (itemStack.getItem() instanceof TieredItem tieredItem && (itemStack.getItem() instanceof SingleHandWeapon || tieredItem.getTier() == Tiers.NETHERITE)) {
            if (tieredItem.getTier() instanceof Tiers tiers) {
                if (tiers == Tiers.NETHERITE)
                    return true;
                return partsLevel < 2;
            }
        }
        return false;
    }

    public boolean draw() {
        return lottery.draw();
    }
}
