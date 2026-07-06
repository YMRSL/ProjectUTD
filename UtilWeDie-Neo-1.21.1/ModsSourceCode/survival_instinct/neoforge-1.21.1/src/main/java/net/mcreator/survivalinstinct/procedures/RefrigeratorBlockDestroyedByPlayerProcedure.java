package net.mcreator.survivalinstinct.procedures;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class RefrigeratorBlockDestroyedByPlayerProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
        if (entity == null) {
            return;
        }
        if ((!new Object(){

            public boolean checkGamemode(Entity _ent) {
                if (_ent instanceof ServerPlayer) {
                    ServerPlayer _serverPlayer = (ServerPlayer)_ent;
                    return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
                }
                if (_ent.level().isClientSide() && _ent instanceof Player) {
                    Player _player = (Player)_ent;
                    return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null && Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SURVIVAL;
                }
                return false;
            }
        }.checkGamemode(entity) || RefrigeratorBlockDestroyedByPlayerProcedure.getSilkTouchLevel(world, itemstack) == 0) && world instanceof ServerLevel) {
            ServerLevel _level = (ServerLevel)world;
            _level.addFreshEntity((Entity)new ExperienceOrb((Level)_level, x, y, z, 3));
        }
    }

    private static int getSilkTouchLevel(LevelAccessor world, ItemStack itemstack) {
        return EnchantmentHelper.getItemEnchantmentLevel(world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), itemstack);
    }
}

