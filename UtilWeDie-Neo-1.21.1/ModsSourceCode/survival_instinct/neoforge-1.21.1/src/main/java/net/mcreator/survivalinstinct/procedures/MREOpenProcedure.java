package net.mcreator.survivalinstinct.procedures;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public class MREOpenProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        ItemStack _setstack;
        Player _player;
        if (entity == null) {
            return;
        }
        if (world instanceof Level) {
            Level _level = (Level)world;
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing((double)x, (double)y, (double)z), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.armor.equip_leather")), SoundSource.NEUTRAL, 1.0f, 1.0f);
            } else {
                _level.playLocalSound(x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.armor.equip_leather")), SoundSource.NEUTRAL, 1.0f, 1.0f, false);
            }
        }
        if (entity instanceof Player) {
            _player = (Player)entity;
            _setstack = new ItemStack((ItemLike)SurvivalInstinctModItems.RICE_COOKIE.get()).copy();
            _setstack.setCount(3);
            ItemHandlerHelper.giveItemToPlayer((Player)_player, (ItemStack)_setstack);
        }
        if (entity instanceof Player) {
            _player = (Player)entity;
            _setstack = new ItemStack((ItemLike)Items.BREAD).copy();
            _setstack.setCount(1);
            ItemHandlerHelper.giveItemToPlayer((Player)_player, (ItemStack)_setstack);
        }
        if (entity instanceof Player) {
            _player = (Player)entity;
            _setstack = new ItemStack((ItemLike)SurvivalInstinctModItems.MILITARY_CAN.get()).copy();
            _setstack.setCount(1);
            ItemHandlerHelper.giveItemToPlayer((Player)_player, (ItemStack)_setstack);
        }
        if (entity instanceof Player) {
            _player = (Player)entity;
            ItemStack _stktoremove = new ItemStack((ItemLike)SurvivalInstinctModItems.MRE.get());
            _player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, (Container)_player.inventoryMenu.getCraftSlots());
        }
    }
}

