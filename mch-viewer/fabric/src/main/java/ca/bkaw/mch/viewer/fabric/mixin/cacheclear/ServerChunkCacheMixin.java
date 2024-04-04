package ca.bkaw.mch.viewer.fabric.mixin.cacheclear;

import ca.bkaw.mch.viewer.fabric.ClearableChunkCache;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin implements ClearableChunkCache {
    @Shadow protected abstract void clearCache();

    @Shadow @Final public ChunkMap chunkMap;

    @Override
    public void mch$clearChunkCache() {
        this.clearCache();
        ((ClearableChunkCache) this.chunkMap).mch$clearChunkCache();
    }
}
