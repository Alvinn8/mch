package ca.bkaw.mch.viewer.fabric.mixin.cacheclear;

import ca.bkaw.mch.viewer.fabric.ClearableChunkCache;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RegionFileStorage.class)
public class RegionFileStorageMixin implements ClearableChunkCache {
    @Shadow @Final private Long2ObjectLinkedOpenHashMap<RegionFile> regionCache;

    @Override
    public void mch$clearChunkCache() {
        this.regionCache.clear();
    }
}
