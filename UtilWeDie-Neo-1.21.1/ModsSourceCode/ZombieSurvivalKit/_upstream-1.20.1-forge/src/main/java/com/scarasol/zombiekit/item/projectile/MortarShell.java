package com.scarasol.zombiekit.item.projectile;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.entity.projectile.MortarShellEntity;
import com.scarasol.zombiekit.item.ZombieKitGeoItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class MortarShell extends ZombieKitGeoItem {

    private final MortarShellEntity.ShellEffect effect;

    public MortarShell(Properties properties, MortarShellEntity.ShellEffect effect) {
        super(properties);
        this.effect = effect;
    }

    public MortarShell(Properties properties) {
        super(properties);
        this.effect = MortarShellEntity.DEFAULT_EFFECT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player entity, InteractionHand hand) {
        if (entity.getVehicle() instanceof MortarEntity mortarEntity && mortarEntity.getShell() == null) {
            mortarEntity.reload(this);
            if (!entity.isCreative())
                entity.getItemInHand(hand).shrink(1);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, entity.getItemInHand(hand));
        }
        return new InteractionResultHolder<>(InteractionResult.FAIL, entity.getItemInHand(hand));
    }

    public MortarShellEntity.ShellEffect getEffect() {
        return effect;
    }

    @Override
    public ResourceLocation getModel() {
        return new ResourceLocation(ZombieKitMod.MODID, "geo/mortar_shell.geo.json");
    }

    @Override
    public ResourceLocation getTexture() {
        return new ResourceLocation(ZombieKitMod.MODID, "textures/item/mortar_shell.png");
    }
}
