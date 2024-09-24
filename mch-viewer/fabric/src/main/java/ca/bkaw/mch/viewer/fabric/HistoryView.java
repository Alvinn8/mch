package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.fs.MchFileSystem;
import ca.bkaw.mch.fs.MchFileSystemProvider;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtFloat;
import ca.bkaw.mch.nbt.NbtInt;
import ca.bkaw.mch.nbt.NbtString;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4d;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorld;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.mixin.MinecraftServerAccess;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HistoryView {
    private final MchViewerFabric mod;
    private final MinecraftServer server;
    private final MchRepository repository;
    private final TrackedWorld trackedWorld;
    private final CachedCommits cachedCommits;
    private final Map<ResourceLocation, DimensionView> dimensionViews = new HashMap<>();
    private CommitInfo commit;

    public HistoryView(MchViewerFabric mod, MinecraftServer server, MchRepository repository, TrackedWorld trackedWorld, CommitInfo commit) throws IOException {
        this.mod = mod;
        this.server = server;
        this.repository = repository;
        this.trackedWorld = trackedWorld;
        this.commit = commit;
        this.cachedCommits = new CachedCommits(repository);
        this.cachedCommits.setup(this.commit);
    }

    public DimensionView viewDimension(ResourceLocation dimensionKey) throws IOException {
        World world = this.getWorld();
        Dimension dimension = this.getDimension(world, dimensionKey);

        Fantasy fantasy = Fantasy.get(this.server);
        RuntimeWorldConfig config = new RuntimeWorldConfig()
            .setGameRule(GameRules.RULE_DAYLIGHT, false)
            .setGenerator(new VoidChunkGenerator(
                this.server.registryAccess().registryOrThrow(Registries.BIOME)
            ));

        ResourceLocation dimensionType = this.getDimensionType(dimensionKey);
        if (dimensionType != null) {
            config.setDimensionType(ResourceKey.create(Registries.DIMENSION_TYPE, dimensionType));
        }

        String key = RandomStringUtils.random(16, "abcdefghijklmnopqrstuvwxyz0123456789");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MchViewerFabric.NAMESPACE, key);
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, id);

        // Fantasy has the concept of temporary worlds, but they force a generated level
        // key. We create the level key ourselves with our namespace and ensure it is
        // handled by mch-fs before creating the world. Then we create a persistent
        // world, but handle file cleanup ourselves.

        // Delete dimension folder on exit.
        Path dimensionPath = ((MinecraftServerAccess) this.server).getSession().getDimensionPath(levelKey);
        Files.createDirectories(dimensionPath);
        FileUtils.forceDeleteOnExit(dimensionPath.toFile());

        // Set up mch-fs before loading the world so that the world's folder is wrapped
        // by mch-fs when the world loads.
        MchFileSystem fileSystem = MchFileSystemProvider.INSTANCE.newFileSystem(
            dimensionPath.toAbsolutePath(), this.repository, this.trackedWorld,
            dimensionKey.toString(), dimension
        );
        Path rootPath = fileSystem.getPath(".").toAbsolutePath().normalize();

        DimensionView dimensionView = new DimensionView(this, fileSystem, rootPath);
        this.mod.registerDimensionView(levelKey, dimensionView);

        // Overwrite the constructor to pass TEMPORARY as the world type.
        RuntimeWorld.Constructor constructor = config.getWorldConstructor();
        config.setWorldConstructor((server0, levelKey0, config0, _style)
            -> constructor.createWorld(server0, levelKey0, config0, RuntimeWorld.Style.TEMPORARY));

        // Create the world.
        RuntimeWorldHandle worldHandle = fantasy.getOrOpenPersistentWorld(id, config);

        dimensionView.setWorldHandle(worldHandle);
        this.dimensionViews.put(dimensionKey, dimensionView);

        dimensionView.getLevel().noSave = true;

        return dimensionView;
    }

    public void setCommit(CommitInfo commit) throws IOException {
        this.commit = commit;

        // Update all dimension views to use the new dimension objects.
        for (Map.Entry<ResourceLocation, DimensionView> entry : new HashMap<>(this.dimensionViews).entrySet()) {
            ResourceLocation dimensionKey = entry.getKey();
            DimensionView oldView = entry.getValue();

            // Create new dimension view
            DimensionView newView = this.viewDimension(dimensionKey);

            // Teleport players from old view to new view
            ServerLevel oldLevel = oldView.getLevel();
            for (ServerPlayer player : new ArrayList<>(oldLevel.players())) {
                ServerLevel newLevel = newView.getLevel();
                player.teleportTo(
                    newLevel, player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot()
                );
                if (!player.getAbilities().flying && player.getAbilities().mayfly) {
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                }
            }

            // Delete old dimension view
            this.mod.unregisterDimensionView(oldLevel.dimension(), oldView);
            oldView.getWorldHandle().delete();
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
     * Get the spawn position and angle of the world being viewed.
     * <p>
     * The vector contains the x, y, z coordinates, and the w component is the angle (yaw).
     *
     * @return The (x, y, z, yaw) vector.
     */
    @NotNull
    public Vector4d getSpawn() {
        try {
            Vector4d pos = this.getWorldSpawn();
            if (pos != null) {
                return pos;
            }
        } catch (IOException ignored) {}

        return new Vector4d(0, 100, 0, 0);
    }

    @Nullable
    private NbtCompound readLevelData() throws IOException {
        World world = this.getWorld();
        Reference20<Dimension> dimensionRef = world.getDimension(Dimension.OVERWORLD);
        if (dimensionRef == null) {
            return null;
        }
        Dimension dimension = dimensionRef.resolve(this.repository);
        Tree tree = dimension.getMiscellaneousFiles().resolve(this.repository);
        Tree.BlobReference blobRef = tree.getFiles().get("level.dat");
        if (blobRef == null) {
            return null;
        }
        Blob levelDatBlob = blobRef.reference().resolve(this.repository);
        DataInputStream stream = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(levelDatBlob.getBytes())));
        NbtCompound nbt = NbtTag.readCompound(stream);
        return (NbtCompound) nbt.get("Data");
    }

    @Nullable
    private Vector4d getWorldSpawn() throws IOException {
        NbtCompound levelData = this.readLevelData();
        if (levelData == null) {
            return null;
        }
        NbtTag nbtX = levelData.get("SpawnX");
        NbtTag nbtY = levelData.get("SpawnY");
        NbtTag nbtZ = levelData.get("SpawnZ");
        NbtTag nbtAngle = levelData.get("SpawnAngle");
        if (nbtX == null || nbtY == null || nbtZ == null) {
            return null;
        }
        int x = ((NbtInt) nbtX).getValue();
        int y = ((NbtInt) nbtY).getValue();
        int z = ((NbtInt) nbtZ).getValue();
        float angle = nbtAngle == null ? 0 : ((NbtFloat) nbtAngle).getValue();
        return new Vector4d(x, y, z, angle);
    }

    @Nullable
    private ResourceLocation getDimensionType(ResourceLocation dimensionKey) throws IOException {
        NbtCompound levelData = this.readLevelData();
        if (levelData == null) {
            return null;
        }
        if (!(levelData.get("WorldGenSettings") instanceof NbtCompound worldGenSettings)) {
            return null;
        }
        if (!(worldGenSettings.get("dimensions") instanceof NbtCompound dimensions)) {
            return null;
        }
        if (!(dimensions.get(dimensionKey.toString()) instanceof NbtCompound dimension)) {
            return null;
        }
        if (!(dimension.get("type") instanceof NbtString type)) {
            return null;
        }
        return ResourceLocation.read(type.getValue()).result().orElse(null);
    }

    /**
     * The commit currently being viewed.
     *
     * @return The commit.ys
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
}
