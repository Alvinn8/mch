package ca.bkaw.mch.viewer.fabric.mixin.cacheclear;

import ca.bkaw.mch.viewer.fabric.ClearableChunkCache;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IOWorker.class)
public class IOWorkerMixin implements ClearableChunkCache {
    @Shadow @Final private RegionFileStorage storage;

    @Override
    public void mch$clearChunkCache() {
        ((ClearableChunkCache) (Object) this.storage).mch$clearChunkCache();
    }
}
