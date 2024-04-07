package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MchViewerFabric implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String NAMESPACE = "mch";

    private static MchViewerFabric instance;
    private volatile FabricServerAudiences adventure;

    private final Map<String, RepoViewerConfig> repoViewerConfigs = new HashMap<>();
    private final Map<ResourceKey<Level>, DimensionView> dimensionViews = new HashMap<>();

    public static MchViewerFabric getInstance() {
        return instance;
    }

    @Override
    public void onInitialize() {
        instance = this;

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> HistoryCommand.register(dispatcher)
        );

        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.adventure = FabricServerAudiences.of(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> this.adventure = null);

        try {
            this.loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.", e);
        }
    }

    private static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("mch-viewer");
    }

    private void loadConfig() throws IOException {
        this.repoViewerConfigs.clear();

        Path configDir = getConfigDir();
        Files.createDirectories(configDir);
        Path configPath = configDir.resolve("config.toml");
        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();

            Config reposConfig = config.get("repos");
            if (reposConfig == null) {
                LOGGER.info("mch-viewer is not configured. Add a repository to config/mch-viewer/config.toml to get started!");
                return;
            }
            for (Config.Entry entry : reposConfig.entrySet()) {
                String key = entry.getKey();
                Config repoConfig = entry.getValue();

                RepoViewerConfig repo = RepoViewerConfig.fromConfig(repoConfig);
                this.repoViewerConfigs.put(key, repo);
            }

            config.save();
        }
    }

    void registerDimensionView(ResourceKey<Level> levelKey, DimensionView dimensionView) {
        this.dimensionViews.put(levelKey, dimensionView);
    }

    void unregisterDimensionView(ResourceKey<Level> levelKey, DimensionView dimensionView) {
        this.dimensionViews.remove(levelKey, dimensionView);
    }

    public HistoryView view(MinecraftServer server, MchRepository repository, TrackedWorld trackedWorld, CommitInfo commit) throws IOException {
        return new HistoryView(this, server, repository, trackedWorld, commit);
    }

    /**
     * Get a {@link DimensionView} associated with a level key of the level is being
     * used by mch to display the history of a dimension.
     *
     * @param levelKey The level key.
     * @return The dimension view, or null.
     */
    @Nullable
    public DimensionView getDimensionView(ResourceKey<Level> levelKey) {
        return this.dimensionViews.get(levelKey);
    }

    /**
     * Get the {@link RepoViewerConfig} by key.
     *
     * @param repoKey The key.
     * @return The repo, or null.
     */
    @Nullable
    public RepoViewerConfig getRepo(String repoKey) {
        return this.repoViewerConfigs.get(repoKey);
    }

    /**
     * Get an unmodifiable set of repo keys.
     *
     * @return The keys.
     */
    public Set<String> getRepoKeys() {
        return Collections.unmodifiableSet(this.repoViewerConfigs.keySet());
    }
}