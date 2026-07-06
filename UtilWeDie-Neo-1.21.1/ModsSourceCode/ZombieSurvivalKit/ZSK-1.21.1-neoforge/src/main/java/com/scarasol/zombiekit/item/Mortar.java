package com.scarasol.zombiekit.item;

import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.init.ZombieKitEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.fml.ModList;

import java.util.List;

public class Mortar extends Item {

    public Mortar(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.zombiekit.mortar.description_1"));
        list.add(Component.translatable("item.zombiekit.mortar.description_2"));
        list.add(Component.translatable("item.zombiekit.mortar.description_3"));
        if (ModList.get().isLoaded("superbwarfare")) {
            list.add(Component.translatable("item.zombiekit.mortar.description_with_sbw"));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        super.useOn(context);
        Player entity = context.getPlayer();
        if (context.getLevel() instanceof ServerLevel server) {
            BlockPos clickPos = context.getClickedPos();
            Direction direction = context.getClickedFace();
            MortarEntity mortar = ZombieKitEntities.MORTAR.get().create(server);
            switch (direction) {
                case UP -> mortar.setPos(clickPos.getX() + 0.5, clickPos.getY() + 1.5, clickPos.getZ() + 0.5);
                case DOWN -> mortar.setPos(clickPos.getX() + 0.5, clickPos.getY() - 0.5, clickPos.getZ() + 0.5);
                case WEST -> mortar.setPos(clickPos.getX() - 0.5, clickPos.getY() + 0.5, clickPos.getZ() + 0.5);
                case EAST -> mortar.setPos(clickPos.getX() + 1.5, clickPos.getY() + 0.5, clickPos.getZ() + 0.5);
                case SOUTH -> mortar.setPos(clickPos.getX() + 0.5, clickPos.getY() + 0.5, clickPos.getZ() + 1.5);
                case NORTH -> mortar.setPos(clickPos.getX() + 0.5, clickPos.getY() + 0.5, clickPos.getZ() - 0.5);
            }
            mortar.finalizeSpawn(server, server.getCurrentDifficultyAt(clickPos), MobSpawnType.SPAWN_EGG, null);
            server.addFreshEntityWithPassengers(mortar);
            if (!entity.getAbilities().instabuild){
                ItemStack itemStack = context.getItemInHand();
                itemStack.setCount(itemStack.getCount() - 1);
            }
        }
        return InteractionResult.CONSUME;
    }
}
