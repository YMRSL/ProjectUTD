package com.scarasol.sona.mixin;

import com.scarasol.sona.accessor.mixin.IChunkAccessor;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Scarasol
 *
 * NeoForge 1.21.1: removed Forge {@code ICapabilityProviderImpl<LevelChunk>} super-interface
 * (the Forge capability provider system no longer exists). The infection compound tag is now
 * carried purely by the {@code @Unique} fields added in {@link ChunkAccessMixin}.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {
    public LevelChunkMixin(ChunkPos pos, UpgradeData upgradeData, LevelHeightAccessor heightAccessor, Registry<Biome> biomeRegistry, long inhabitedTime, @Nullable LevelChunkSection[] sections, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
    }

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("TAIL"))
    private void sona$LevelChunk(ServerLevel serverLevel, ProtoChunk protoChunk, LevelChunk.PostLoadProcessor postLoadProcessor, CallbackInfo ci) {
        IChunkAccessor protoChunkAccessor = IChunkAccessor.fromLevelChunk(protoChunk);
        IChunkAccessor chunkAccessor = IChunkAccessor.fromLevelChunk(this);
        chunkAccessor.setSonaCompoundTag(protoChunkAccessor.getSonaCompoundTag());
    }
}
