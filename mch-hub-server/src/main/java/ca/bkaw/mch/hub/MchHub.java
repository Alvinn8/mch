package ca.bkaw.mch.hub;

import ca.bkaw.mch.repository.MchRepository;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class MchHub {
    public static void start(InetSocketAddress addr) throws IOException {
        MchHubServer server = new MchHubServer();

        Path configPath = Path.of("mch-hub.toml");
        try (FileConfig config = FileConfig.of(configPath)) {
            config.load();

            Config reposConfig = config.get("repos");
            if (reposConfig == null) {
                System.out.println("mch-hub-server is not configured. Add a repository to mch-hub.toml to get started!");
                return;
            }
            for (Config.Entry entry : reposConfig.entrySet()) {
                String key = entry.getKey();
                Config repoConfig = entry.getValue();

                String pathStr = repoConfig.get("path");
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
                server.addRepo(key, repository);
            }
        }


        server.start(addr);
        System.out.println("Starting mch-hub-server on port " + addr.getPort());
    }

    public static void main(String[] args) throws IOException {
        int port = 8000;
        start(new InetSocketAddress(port));
    }
}
