package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorld;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

public class MchViewerFabric implements ModInitializer {
    private static MchViewerFabric instance;

    private final Map<ResourceKey<Level>, HistoryView> historyViews = new HashMap<>();

    @Override
    public void onInitialize() {
        instance = this;

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> HistoryCommand.register(dispatcher)
        );
    }

    public static MchViewerFabric getInstance() {
        return instance;
    }

    public HistoryView view(MinecraftServer server, MchRepository repository, TrackedWorld trackedWorld) throws IOException {
        HistoryView view = new HistoryView(server, repository, trackedWorld);

        Fantasy fantasy = Fantasy.get(server);
        RuntimeWorldConfig config = new RuntimeWorldConfig()
            .setGameRule(GameRules.RULE_DAYLIGHT, false)
            .setGenerator(new VoidChunkGenerator(
                server.registryAccess().registryOrThrow(Registries.BIOME)
            ));

        RuntimeWorld.Constructor constructor = config.getWorldConstructor();
        config.setWorldConstructor((minecraftServer, levelKey, runtimeWorldConfig, style) -> {
            // Now that we know the random key that we were given by Fantasy, we can
            // get the dimension path. This path needs to be wrapped by mch-fs.
            try {
                view.setupMchFs(minecraftServer, levelKey);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to set up mch-fs", e);
            }
            this.historyViews.put(levelKey, view);
            // Call the normal constructor and create the world.
            return constructor.createWorld(minecraftServer, levelKey, runtimeWorldConfig, style);
        });

        // Create the world.
        RuntimeWorldHandle worldHandle = fantasy.openTemporaryWorld(config);
        view.setWorldHandle(worldHandle);

        return view;
    }

    /**
     * Get a {@link HistoryView} associated with a level key.
     *
     * @param levelKey The level key.
     * @return The history view.
     */
    @Nullable
    public HistoryView getHistoryView(ResourceKey<Level> levelKey) {
        return this.historyViews.get(levelKey);
    }
}