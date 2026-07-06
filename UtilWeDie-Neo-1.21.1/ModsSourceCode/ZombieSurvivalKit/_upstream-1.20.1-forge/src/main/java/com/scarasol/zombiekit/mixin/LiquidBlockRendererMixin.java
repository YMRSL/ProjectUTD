package com.scarasol.zombiekit.mixin;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.client.shaders.ThermalShader;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    @Inject(
            method = "tesselate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
            at = @At("HEAD")
    )
    private void zombiekit$recordLuminousFluid(BlockAndTintGetter level, BlockPos pos, VertexConsumer vertexConsumer, BlockState state, FluidState fluidState, CallbackInfo ci) {
        int emission = state.getLightEmission(level, pos);
        if (emission > 0) {
            ThermalShader.recordLuminousBlock(level, state, pos, emission);
        }
    }
}