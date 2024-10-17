package ca.bkaw.mch.viewer.fabric;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.RepositoryAccess;
import ca.bkaw.mch.repository.remote.RemoteRepositoryAccess;
import com.electronwill.nightconfig.core.Config;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4d;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Configuration about a repository that can be viewed by mch viewer.
 */
public class RepoViewerConfig {
    private final RepositoryAccess repositoryAccess;
    private final Sha1 trackedWorldSha1;
    @Nullable
    private Vector4d spawnOverride;

    public RepoViewerConfig(RepositoryAccess repositoryAccess, Sha1 trackedWorldSha1) {
        this.repositoryAccess = repositoryAccess;
        this.trackedWorldSha1 = trackedWorldSha1;
    }

    public static RepoViewerConfig fromConfig(Config config) throws IOException {
        RepositoryAccess repositoryAccess = getRepositoryAccess(config);

        // Get tracked world to view
        boolean hasWorld = config.contains("world");
        List<Sha1> trackedWorlds = repositoryAccess.getTrackedWorlds();
        if (!hasWorld && trackedWorlds.size() != 1) {
            throw new RuntimeException("Must specify which world to view.");
        }
        Sha1 trackedWorldSha1 = null;
        if (hasWorld) {
            String worldStr = config.get("world");
            // Try to get by name
            trackedWorldSha1 = repositoryAccess.getTrackedWorld(worldStr);
            if (trackedWorldSha1 == null) {
                Sha1 worldSha1 = Sha1.fromString(worldStr);
                if (trackedWorlds.contains(worldSha1)) {
                    trackedWorldSha1 = worldSha1;
                }
            }
        } else {
            trackedWorldSha1 = trackedWorlds.iterator().next();
        }
        if (trackedWorldSha1 == null) {
            throw new RuntimeException("Unable to find tracked world.");
        }

        RepoViewerConfig repo = new RepoViewerConfig(repositoryAccess, trackedWorldSha1);

        if (config.contains("spawn")) {
            repo.readSpawnOverride(config.get("spawn"));
        }

        return repo;
    }

    private static RepositoryAccess getRepositoryAccess(Config config) throws IOException {
        if (config.contains("path")) {
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

            // Use the repository itself as the RepositoryAccess
            return repository;
        }
        if (config.contains("url")) {
            String strUrl = config.get("url");
            URL url = URI.create(strUrl).toURL();
            return new RemoteRepositoryAccess(url, "N/A");
        }
        throw new RuntimeException("A repo needs to either have a 'path' or 'url'.");
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

    public RepositoryAccess getRepositoryAccess() {
        return this.repositoryAccess;
    }

    public Sha1 getTrackedWorldSha1() {
        return this.trackedWorldSha1;
    }

    @Nullable
    public Vector4d getSpawnOverride() {
        return this.spawnOverride;
    }
}
