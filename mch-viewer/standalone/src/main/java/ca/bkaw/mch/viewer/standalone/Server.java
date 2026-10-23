package ca.bkaw.mch.viewer.standalone;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private final ServerSocket serverSocket;
    private final List<PlayerConnection> connections = new ArrayList<>();

    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    public void start() {
        System.out.println("Server started on port " + this.serverSocket.getLocalPort());
        while (true) {
            try {
                PlayerConnection connection = new PlayerConnection(serverSocket.accept());
                this.connections.add(connection);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
