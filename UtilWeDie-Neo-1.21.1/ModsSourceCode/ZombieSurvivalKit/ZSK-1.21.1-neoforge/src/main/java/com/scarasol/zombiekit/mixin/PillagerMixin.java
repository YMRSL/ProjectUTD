package com.scarasol.zombiekit.mixin;

import com.scarasol.zombiekit.item.weapon.Flamethrower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Pillager.class)
public abstract class PillagerMixin extends AbstractIllager implements CrossbowAttackMob, InventoryCarrier {

    protected PillagerMixin(EntityType<? extends AbstractIllager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getArmPose", at = @At("RETURN"), cancellable = true)
    private void onGetArmPose(CallbackInfoReturnable<AbstractIllager.IllagerArmPose> cir){
        if (this.isHolding(is -> is.getItem() instanceof Flamethrower)){
            cir.setReturnValue(AbstractIllager.IllagerArmPose.CROSSBOW_HOLD);
        }
    }
}
