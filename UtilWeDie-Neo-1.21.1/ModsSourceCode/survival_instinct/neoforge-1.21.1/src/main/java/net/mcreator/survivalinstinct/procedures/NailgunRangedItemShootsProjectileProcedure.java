package net.mcreator.survivalinstinct.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NailgunRangedItemShootsProjectileProcedure {
    public static void execute(Entity entity, ItemStack itemstack) {
        if (entity == null) {
            return;
        }
        if (entity instanceof Player) {
            Player _player = (Player)entity;
            _player.getCooldowns().addCooldown(itemstack.getItem(), 3);
        }
    }
}

