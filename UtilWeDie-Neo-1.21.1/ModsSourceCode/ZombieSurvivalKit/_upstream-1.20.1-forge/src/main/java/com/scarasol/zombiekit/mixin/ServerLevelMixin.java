package com.scarasol.zombiekit.mixin;

import com.scarasol.zombiekit.api.MortarLevel;
import com.scarasol.zombiekit.manager.MortarManager;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements WorldGenLevel, MortarLevel {


    @Shadow public abstract ServerChunkCache getChunkSource();

    @Unique
    private final MortarManager mortarManager = new MortarManager(this);

    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean b1, boolean b2, long l1, int i1) {
        super(levelData,resourceKey , registryAccess, holder, supplier, b1, b2, l1, i1);
    }

    @Override
    public MortarManager getMortarManager() {
        return mortarManager;
    }

    @Override
    public ServerChunkCache getServerChunkCache() {
        return getChunkSource();
    }

}
