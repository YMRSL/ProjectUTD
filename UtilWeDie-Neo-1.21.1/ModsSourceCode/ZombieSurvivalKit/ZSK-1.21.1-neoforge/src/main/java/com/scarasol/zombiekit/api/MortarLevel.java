package com.scarasol.zombiekit.api;

import com.scarasol.zombiekit.manager.MortarManager;
import net.minecraft.server.level.ServerChunkCache;

public interface MortarLevel {
    MortarManager getMortarManager();

    ServerChunkCache getServerChunkCache();
}
