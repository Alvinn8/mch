package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.fs.MchFileSystem;
import ca.bkaw.mch.fs.MchFileSystemProvider;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtFloat;
import ca.bkaw.mch.nbt.NbtInt;
import ca.bkaw.mch.nbt.NbtString;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.repository.DimensionAccess;
import ca.bkaw.mch.repository.RepositoryAccess;
import ca.bkaw.mch.util.StringPath;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.Commands;
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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HistoryView {
    private final MchViewerFabric mod;
    private final MinecraftServer server;
    private final RepositoryAccess repositoryAccess;
    private final Sha1 trackedWorldSha1;
    private final CachedCommits cachedCommits;
    private final Map<ResourceLocation, DimensionView> dimensionViews = new HashMap<>();
    private CommitInfo commit;

    public HistoryView(MchViewerFabric mod, MinecraftServer server, RepositoryAccess repositoryAccess, Sha1 trackedWorldSha1, CommitInfo commit) throws IOException {
        this.mod = mod;
        this.server = server;
        this.repositoryAccess = repositoryAccess;
        this.trackedWorldSha1 = trackedWorldSha1;
        this.commit = commit;
        this.cachedCommits = new CachedCommits(repositoryAccess);
        this.cachedCommits.setup(this.commit);
    }

    public DimensionView viewDimension(ResourceLocation dimensionKey) throws IOException {
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
        DimensionAccess dimensionAccess = this.repositoryAccess.accessDimension(
            this.commit.hash(), this.trackedWorldSha1, dimensionKey.toString()
        );
        MchFileSystem fileSystem = MchFileSystemProvider.INSTANCE.newFileSystem(
            dimensionPath.toAbsolutePath(), dimensionAccess
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
                newView.preloadArea(player.getX(), player.getZ(), player).thenRun(() -> this.server.executeIfPossible(() -> {
                    player.teleportTo(
                        newLevel, player.getX(), player.getY(), player.getZ(),
                        player.getYRot(), player.getXRot()
                    );
                    if (!player.getAbilities().flying && player.getAbilities().mayfly) {
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                    }
                    this.onStartViewing(player);

                    if (oldLevel.players().isEmpty()) {
                        // Delete old dimension view
                        this.mod.unregisterDimensionView(oldLevel.dimension(), oldView);
                        oldView.getWorldHandle().delete();
                    }
                }));
            }
        }
    }

    /**
     * This method is called, and should be called, when the player has teleported into
     * a dimensions where history is being viewed.
     *
     * @param player The player that entered.
     */
    public void onStartViewing(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Commands commands = server.getCommands();
        commands.performPrefixedCommand(player.createCommandSourceStack(), "history log");
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
        DimensionAccess dimensionAccess = this.repositoryAccess.accessDimension(this.commit.hash(), this.trackedWorldSha1, Dimension.OVERWORLD);
        if (dimensionAccess == null) {
            return null;
        }
        try (InputStream stream = dimensionAccess.restoreFile(StringPath.of("level.dat"))) {
            if (stream == null) {
                return null;
            }
            DataInputStream dataInput = new DataInputStream(new GZIPInputStream(stream));
            NbtCompound nbt = NbtTag.readCompound(dataInput);
            return (NbtCompound) nbt.get("Data");
        }
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

    public List<String> getDimensions() throws IOException {
        return this.repositoryAccess.getDimensions(this.commit.hash(), this.trackedWorldSha1);
    }

    /**
     * The commit currently being viewed.
     *
     * @return The commit.
     */
    public CommitInfo getCommit() {
        return this.commit;
    }

    public RepositoryAccess getRepositoryAccess() {
        return this.repositoryAccess;
    }

    public CachedCommits getCachedCommits() {
        return this.cachedCommits;
    }

    public Sha1 getTrackedWorldSha1() {
        return this.trackedWorldSha1;
    }
}
