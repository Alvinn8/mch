package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.fs.MchFileSystem;
import ca.bkaw.mch.fs.MchPath;
import ca.bkaw.mch.object.dimension.Dimension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.io.IOException;
import java.nio.file.Path;

public final class DimensionView {
    private final HistoryView parent;
    private final RuntimeWorldHandle worldHandle;
    private final MchFileSystem fileSystem;
    private final Path rootPath;
    private final ResourceLocation dimensionKey;
    private Dimension dimensionObject;

    public DimensionView(HistoryView parent, RuntimeWorldHandle worldHandle, Dimension dimensionObject,
                         ResourceLocation dimensionKey, MchFileSystem fileSystem, Path rootPath) {
        this.parent = parent;
        this.worldHandle = worldHandle;
        this.dimensionObject = dimensionObject;
        this.dimensionKey = dimensionKey;
        this.fileSystem = fileSystem;
        this.rootPath = rootPath;
    }

    public HistoryView getParent() {
        return this.parent;
    }

    public Path wrapPath(Path original) {
        // We basically want to check of the "original" path starts with the root path.
        // First, the "original" path needs to be made absolute since the root path is
        // also absolute.
        // The paths are also from different file system providers (default and mch),
        // so we unwrap the root path (mch provider) to the default file system, so
        // that they can be compared with the regular startsWith method.
        //
        Path normalized = original.toAbsolutePath().normalize();
        Path unwrapped = MchPath.unwrap(this.rootPath);
        if (normalized.startsWith(unwrapped)) {
            // The original path should be wrapped
            return new MchPath(this.fileSystem, original);
        }
        return original;
    }

    public void setDimension(Dimension dimensionObject) throws IOException {
        this.dimensionObject = dimensionObject;
        this.update();
    }

    private void update() throws IOException {
        if (this.fileSystem == null) {
            return;
        }
        this.fileSystem.setWorld(this.parent.getTrackedWorld(), this.dimensionKey.toString(), this.dimensionObject);

        // Clear chunk caches
        ServerLevel level = this.worldHandle.asWorld();
        level.save(null, true, true); // flush, disable saving
        ServerChunkCache chunkCache = level.getChunkSource();
        chunkCache.save(true); // save and flush
        ((ClearableChunkCache) chunkCache).mch$clearChunkCache();
    }

}
