package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.fs.MchFileSystem;
import ca.bkaw.mch.fs.MchPath;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.mixin.MinecraftServerAccess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<Void> preloadArea(double blockX, double blockZ, @Nullable ServerPlayer processTracker) {
        if (processTracker != null) {
            processTracker.sendMessage(Component.text("Preloading, please wait..."));
        }
        return CompletableFuture.runAsync(() -> {
            Set<String> regionFileNames = this.getPreloadRegionFiles(blockX, blockZ);
            System.out.println("Preloading " + regionFileNames.size() + " region files: " + regionFileNames);

            ServerLevel world = this.worldHandle.asWorld();
            // The mixin will intercept this call and ensure that the path is wrapped by mch-fs.
            Path dimensionPath = ((MinecraftServerAccess) world.getServer()).getSession().getDimensionPath(world.dimension());
            Path regionFolder = dimensionPath.resolve("region");
            Path entitiesFolder = dimensionPath.resolve("entities");

            int completed = 0;
            for (String fileName : regionFileNames) {
                Path regionPath = regionFolder.resolve(fileName);
                Path entitiesPath = entitiesFolder.resolve(fileName);
                Files.exists(regionPath);
                Files.exists(entitiesPath);
                completed++;
                if (processTracker != null) {
                    int percentage = (int) Math.round(100.0 * completed / regionFileNames.size());
                    processTracker.sendMessage(Component.text("... " + percentage + "%"));
                }
            }
        }).exceptionally(e -> {
            MchViewerFabric.LOGGER.error("Failed to preload chunks", e);
            return null;
        });
    }

    private Set<String> getPreloadRegionFiles(double blockX, double blockZ) {
        Set<String> regionFileNames = new HashSet<>();

        // We always want to preload the region file the player will stand in.
        int middleRegionX = (int) blockX >> 9;
        int middleRegionZ = (int) blockZ >> 9;
        String middleFileName = "r." + middleRegionX + "." + middleRegionZ + ".mca";
        regionFileNames.add(middleFileName);

        // Preload surrounding region files if they are close enough.
        int dist;
        final int regionBlockSize = 32 * 16;

        // Check view-distance and preload accordingly.
        MinecraftServer server = this.worldHandle.asWorld().getServer();
        if (server instanceof DedicatedServer dedicatedServer) {
            // Add one chunk to have some margin
            int blockViewDistance = dedicatedServer.getProperties().viewDistance * 16 + 16;
            dist = Mth.clamp(blockViewDistance, 6 * 16, 16 * 16);
        } else {
            dist = 16 * 16;
        }
        System.out.println("dist = " + dist);

        for (int dx = -dist; dx <= dist + regionBlockSize; dx += regionBlockSize) {
            for (int dz = -dist; dz <= dist + regionBlockSize; dz += regionBlockSize) {
                int regionX = (int) (blockX + dx) >> 9;
                int regionZ = (int) (blockZ + dz) >> 9;
                String fileName = "r." + regionX + "." + regionZ + ".mca";
                regionFileNames.add(fileName);
            }
        }
        return regionFileNames;
    }
}
