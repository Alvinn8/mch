package ca.bkaw.mch.hub;

import ca.bkaw.mch.repository.MchRepository;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MchHubServer {
    private final HttpServer httpServer;

    public MchHubServer() throws IOException {
        this.httpServer = HttpServer.create();
        this.httpServer.setExecutor(null);
    }

    public void start(InetSocketAddress addr) throws IOException {
        this.httpServer.bind(addr, 0);
        this.httpServer.start();
    }

    public void addRepo(String identifier, MchRepository repository) {
        RepoHandler handler = new RepoHandler(repository);
        this.httpServer.createContext("/repo/" + identifier + "/v1/getHeadCommit", handler::getHeadCommit);
        this.httpServer.createContext("/repo/" + identifier + "/v1/getTrackedWorlds", handler::getTrackedWorlds);
        this.httpServer.createContext("/repo/" + identifier + "/v1/getTrackedWorld", handler::getTrackedWorld);
        this.httpServer.createContext("/repo/" + identifier + "/v1/getDimensions", handler::getDimensions);
        this.httpServer.createContext("/repo/" + identifier + "/v1/accessCommit", handler::accessCommit);
        this.httpServer.createContext("/repo/" + identifier + "/v1/restoreFile", handler::restoreFile);
        this.httpServer.createContext("/repo/" + identifier + "/v1/list", handler::list);
    }

    public void stop(int delay) {
        this.httpServer.stop(delay);
    }
}
