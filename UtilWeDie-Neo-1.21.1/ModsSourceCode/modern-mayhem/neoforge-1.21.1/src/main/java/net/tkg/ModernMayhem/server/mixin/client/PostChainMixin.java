package net.tkg.ModernMayhem.server.mixin.client;

import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.tkg.ModernMayhem.server.mixinaccessor.PostChainAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={PostChain.class})
public abstract class PostChainMixin
implements AutoCloseable,
PostChainAccess {
    @Shadow
    @Final
    private List<PostPass> passes;

    @Override
    public List<PostPass> test_master$getPasses() {
        return this.passes;
    }
}

