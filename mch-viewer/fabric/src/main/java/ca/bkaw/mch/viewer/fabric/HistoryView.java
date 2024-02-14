package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.fs.MchFileSystem;
import ca.bkaw.mch.fs.MchFileSystemProvider;
import ca.bkaw.mch.fs.MchPath;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.mixin.MinecraftServerAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HistoryView {
    private final MchRepository repository;
    private final TrackedWorld trackedWorld;
    private RuntimeWorldHandle worldHandle;
    private ResourceLocation dimensionKey;
    private Commit commit;
    private Dimension dimensionView;
    private MchFileSystem fileSystem;
    private Path rootPath;

    public HistoryView(MinecraftServer server, MchRepository repository, TrackedWorld trackedWorld) throws IOException {
        this.repository = repository;
        this.trackedWorld = trackedWorld;

        // Reference20<Commit> headCommitRef = this.repository.getHeadCommit();
        // if (headCommitRef == null) {
        //     throw new IllegalArgumentException("Repository is empty");
        // }

        Reference20<Commit> commitRef = new Reference20<>(ObjectStorageTypes.COMMIT, Sha1.fromString("d73512c201f7358b34a77bce302f16f9b4a97d6d"));

        this.commit = commitRef.resolve(this.repository);

        this.setDimensionKey(Level.OVERWORLD.location());
    }

    /**
     * Set the key of the dimension to view.
     *
     * @param dimensionKey The dimension key.
     * @throws IOException If an I/O error occurs.
     * @throws IllegalArgumentException If the dimension key was not present in the commit.
     */
    public void setDimensionKey(ResourceLocation dimensionKey) throws IOException {
        this.dimensionKey = dimensionKey;
        World world = this.getWorld();
        Reference20<Dimension> dimensionRef = world.getDimension(dimensionKey.toString());
        if (dimensionRef == null) {
            throw new IllegalArgumentException(
                "The dimension " + dimensionKey + " was not present in the commit."
            );
        }
        this.dimensionView = dimensionRef.resolve(this.repository);
        this.update();
    }

    /**
     * Get the {@link World} object from the current commit.
     *
     * @return The world object.
     * @throws IOException If an I/O error occurs.
     */
    public World getWorld() throws IOException {
        WorldContainer worldContainer = this.commit.getWorldContainer().resolve(this.repository);
        Reference20<World> worldRef = worldContainer.getWorld(this.trackedWorld.getId());
        if (worldRef == null) {
            throw new IllegalStateException(
                "The world " + this.trackedWorld.getId() + " ("
                    + this.trackedWorld.getName()
                    + ") was not present in the commit."
            );
        }
        return worldRef.resolve(this.repository);
    }

    /**
     * The commit currently being viewed.
     *
     * @return The commit.
     */
    public Commit getCommit() {
        return this.commit;
    }

    /**
     * Set the Fantasy {@link RuntimeWorldHandle} that this history view uses to render
     * the history.
     *
     * @param world The world handle.
     */
    public void setWorldHandle(RuntimeWorldHandle world) {
        if (this.worldHandle != null) {
            throw new IllegalStateException("Cannot change world handle.");
        }
        this.worldHandle = world;
    }

    private void update() {
        // TODO cause the game to clear chunk caches etc.
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

    public void setupMchFs(MinecraftServer server, ResourceKey<Level> levelKey) throws IOException {
        LevelStorageSource.LevelStorageAccess session = ((MinecraftServerAccess) server).getSession();

        Path dimensionPath = session.getDimensionPath(levelKey).toAbsolutePath();
        Files.createDirectories(dimensionPath);

        this.fileSystem = MchFileSystemProvider.INSTANCE.newFileSystem(
            dimensionPath, this.repository, this.trackedWorld,
            this.dimensionKey.toString(), this.dimensionView
        );
        this.rootPath = this.fileSystem.getPath(".").toAbsolutePath().normalize();
    }

    public boolean isReady() {
        return this.fileSystem != null;
    }

    public RuntimeWorldHandle getWorldHandle() {
        return this.worldHandle;
    }
}
