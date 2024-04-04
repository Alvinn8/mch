package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.fs.MchFileSystem;
import ca.bkaw.mch.fs.MchFileSystemProvider;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorld;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.mixin.MinecraftServerAccess;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class HistoryView {
    private final MchRepository repository;
    private final TrackedWorld trackedWorld;
    private final CachedCommits cachedCommits;
    private final Map<ResourceLocation, DimensionView> dimensionViews = new HashMap<>();
    private CommitInfo commit;

    public HistoryView(MchViewerFabric mod, MinecraftServer server, MchRepository repository, TrackedWorld trackedWorld, CommitInfo commit) throws IOException {
        this.repository = repository;
        this.trackedWorld = trackedWorld;
        this.commit = commit;
        this.cachedCommits = new CachedCommits(repository);
        this.cachedCommits.setup(this.commit);

        this.viewDimension(mod, server, Level.OVERWORLD.location());
    }

    public void viewDimension(MchViewerFabric mod, MinecraftServer server, ResourceLocation dimensionKey) throws IOException {
        World world = this.getWorld();
        Dimension dimension = this.getDimension(world, dimensionKey);

        Fantasy fantasy = Fantasy.get(server);
        RuntimeWorldConfig config = new RuntimeWorldConfig()
            .setGameRule(GameRules.RULE_DAYLIGHT, false)
            .setGenerator(new VoidChunkGenerator(
                server.registryAccess().registryOrThrow(Registries.BIOME)
            ));

        String key = RandomStringUtils.random(16, "abcdefghijklmnopqrstuvwxyz0123456789");
        ResourceLocation id = new ResourceLocation(MchViewerFabric.NAMESPACE, key);
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, id);

        // Fantasy has the concept of temporary worlds, but they force a generated level
        // key. We create the level key ourselves with our namespace and ensure it is
        // handled by mch-fs before creating the world. Then we create a persistent
        // world, but handle file cleanup ourselves.

        // Delete dimension folder on exit.
        Path dimensionPath = ((MinecraftServerAccess) server).getSession().getDimensionPath(levelKey);
        Files.createDirectories(dimensionPath);
        FileUtils.forceDeleteOnExit(dimensionPath.toFile());

        // Set up mch-fs before loading the world so that the world's folder is wrapped
        // by mch-fs when the world loads.
        MchFileSystem fileSystem = MchFileSystemProvider.INSTANCE.newFileSystem(
            dimensionPath.toAbsolutePath(), this.repository, this.trackedWorld,
            levelKey.toString(), dimension
        );
        Path rootPath = fileSystem.getPath(".").toAbsolutePath().normalize();

        // Overwrite the constructor to pass TEMPORARY as the world type.
        RuntimeWorld.Constructor constructor = config.getWorldConstructor();
        config.setWorldConstructor((server0, levelKey0, config0, _style)
            -> constructor.createWorld(server0, levelKey0, config0, RuntimeWorld.Style.TEMPORARY));

        // Create the world.
        RuntimeWorldHandle worldHandle = fantasy.getOrOpenPersistentWorld(id, config);
        worldHandle.asWorld().noSave = true;

        DimensionView dimensionView = new DimensionView(this, worldHandle, dimension, dimensionKey, fileSystem, rootPath);

        // TODO we need to call register before getOrOpenPersistentWorld
        mod.registerDimensionView(levelKey, dimensionView);
        this.dimensionViews.put(dimensionKey, dimensionView);
    }

    public void setCommit(CommitInfo commit) throws IOException {
        this.commit = commit;

        // Update all dimension views to use the new dimension objects.
        World world = this.getWorld();
        for (Map.Entry<ResourceLocation, DimensionView> entry : this.dimensionViews.entrySet()) {
            ResourceLocation dimensionKey = entry.getKey();
            DimensionView dimensionView = entry.getValue();
            Dimension dimensionObject = this.getDimension(world, dimensionKey);
            dimensionView.setDimension(dimensionObject);
        }
    }

    /**
     * Get the {@link World} object from the current commit.
     *
     * @return The world object.
     * @throws IOException If an I/O error occurs.
     */
    public World getWorld() throws IOException {
        WorldContainer worldContainer = this.commit.commit().getWorldContainer().resolve(this.repository);
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

    private Dimension getDimension(World world, ResourceLocation dimensionKey) throws IOException {
        Reference20<Dimension> dimensionRef = world.getDimension(dimensionKey.toString());
        if (dimensionRef == null) {
            throw new IllegalArgumentException(
                "The dimension " + dimensionKey + " was not present in the commit."
            );
        }
        return dimensionRef.resolve(this.repository);
    }

    /**
     * The commit currently being viewed.
     *
     * @return The commit.
     */
    public CommitInfo getCommit() {
        return this.commit;
    }

    public MchRepository getRepository() {
        return this.repository;
    }

    public CachedCommits getCachedCommits() {
        return this.cachedCommits;
    }

    public TrackedWorld getTrackedWorld() {
        return this.trackedWorld;
    }
}
