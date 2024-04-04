package ca.bkaw.mch.viewer.fabric.mixin.cacheclear;

import ca.bkaw.mch.viewer.fabric.ClearableChunkCache;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin extends ChunkStorage implements ClearableChunkCache {
    public ChunkMapMixin() {
        super(null, null, false);
    }

    @Override
    public void mch$clearChunkCache() {
        ((ClearableChunkCache) this.chunkScanner()).mch$clearChunkCache();
    }
}
