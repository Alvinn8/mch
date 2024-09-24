package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import com.electronwill.nightconfig.core.Config;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration about a repository that can be viewed by mch viewer.
 */
public class RepoViewerConfig {
    private final MchRepository repository;
    private final TrackedWorld trackedWorld;
    @Nullable
    private Vector4d spawnOverride;

    public RepoViewerConfig(MchRepository repository, TrackedWorld trackedWorld) {
        this.repository = repository;
        this.trackedWorld = trackedWorld;
    }

    public static RepoViewerConfig fromConfig(Config config) throws IOException {
        String pathStr = config.get("path");
        Path path = Path.of(pathStr);
        Path mchPath = path.resolve("mch");
        if (Files.exists(mchPath)) {
            path = mchPath;
        }
        MchRepository repository = new MchRepository(path);
        if (!repository.exists()) {
            throw new RuntimeException("There is no repository at the path: '" + pathStr + "'.");
        }
        repository.readConfiguration();
        MchConfiguration repoConfig = repository.getConfiguration();

        // Get tracked world to view
        boolean hasWorld = config.contains("world");
        if (!hasWorld && repoConfig.getTrackedWorlds().size() != 1) {
            throw new RuntimeException("Must specify which world to view.");
        }
        TrackedWorld trackedWorld;
        if (hasWorld) {
            String worldStr = config.get("world");
            // Try to get by name
            trackedWorld = repoConfig.getTrackedWorld(worldStr);
            if (trackedWorld == null) {
                Sha1 worldSha1 = Sha1.fromString(worldStr);
                trackedWorld = repoConfig.getTrackedWorld(worldSha1);
            }
        } else {
            trackedWorld = repoConfig.getTrackedWorlds().iterator().next();
        }
        if (trackedWorld == null) {
            throw new RuntimeException("Unable to find tracked world.");
        }

        RepoViewerConfig repo = new RepoViewerConfig(repository, trackedWorld);

        if (config.contains("spawn")) {
            repo.readSpawnOverride(config.get("spawn"));
        }

        return repo;
    }

    private void readSpawnOverride(Config config) {
        Number x = config.get("x");
        Number y = config.get("y");
        Number z = config.get("z");
        Number yaw = config.getOrElse("yaw", 0);
        this.spawnOverride = new Vector4d(
            x.doubleValue(), y.doubleValue(), z.doubleValue(), yaw.doubleValue()
        );
    }

    public MchRepository getRepository() {
        return this.repository;
    }

    public TrackedWorld getTrackedWorld() {
        return this.trackedWorld;
    }

    @Nullable
    public Vector4d getSpawnOverride() {
        return this.spawnOverride;
    }
}
