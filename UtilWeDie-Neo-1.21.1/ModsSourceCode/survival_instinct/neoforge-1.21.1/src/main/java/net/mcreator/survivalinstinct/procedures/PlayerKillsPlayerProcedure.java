package net.mcreator.survivalinstinct.procedures;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class PlayerKillsPlayerProcedure {
    public static void execute(DamageSource damagesource, Entity entity, Entity sourceentity) {
        if (damagesource == null || entity == null || sourceentity == null) {
            return;
        }
        if (damagesource.is(DamageTypes.PLAYER_ATTACK) && entity instanceof Player && sourceentity instanceof Player) {
            Player _player = (Player)sourceentity;
            ItemStack _setstack = new ItemStack((ItemLike)SurvivalInstinctModItems.MONEY.get()).copy();
            _setstack.setCount(1);
            ItemHandlerHelper.giveItemToPlayer((Player)_player, (ItemStack)_setstack);
        }
    }
}

