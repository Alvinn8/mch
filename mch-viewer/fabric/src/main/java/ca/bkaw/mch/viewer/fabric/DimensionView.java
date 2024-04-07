package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.fs.MchFileSystem;
import ca.bkaw.mch.fs.MchPath;
import net.minecraft.server.level.ServerLevel;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.nio.file.Path;

public final class DimensionView {
    private final HistoryView parent;
    private final MchFileSystem fileSystem;
    private final Path rootPath;
    private RuntimeWorldHandle worldHandle;

    public DimensionView(HistoryView parent, MchFileSystem fileSystem, Path rootPath) {
        this.parent = parent;
        this.fileSystem = fileSystem;
        this.rootPath = rootPath;
    }

    public HistoryView getParent() {
        return this.parent;
    }

    public void setWorldHandle(RuntimeWorldHandle worldHandle) {
        if (this.worldHandle != null) {
            throw new IllegalStateException("Cannot change world handle.");
        }
        this.worldHandle = worldHandle;
    }

    public RuntimeWorldHandle getWorldHandle() {
        return this.worldHandle;
    }

    public ServerLevel getLevel() {
        return this.worldHandle.asWorld();
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
}
