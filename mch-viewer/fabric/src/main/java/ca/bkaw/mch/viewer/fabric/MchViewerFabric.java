package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MchViewerFabric implements ModInitializer {
    public static final String NAMESPACE = "mch";

    private static MchViewerFabric instance;
    private volatile FabricServerAudiences adventure;

    private final Map<ResourceKey<Level>, DimensionView> dimensionViews = new HashMap<>();

    @Override
    public void onInitialize() {
        instance = this;

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> HistoryCommand.register(dispatcher)
        );

        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.adventure = FabricServerAudiences.of(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> this.adventure = null);
    }

    public static MchViewerFabric getInstance() {
        return instance;
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
     * Get a {@link DimensionView} associated with a level key.
     *
     * @param levelKey The level key.
     * @return The history view.
     */
    @Nullable
    public DimensionView getDimensionView(ResourceKey<Level> levelKey) {
        return this.dimensionViews.get(levelKey);
    }
}