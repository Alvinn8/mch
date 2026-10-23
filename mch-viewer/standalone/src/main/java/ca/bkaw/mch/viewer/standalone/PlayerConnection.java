package ca.bkaw.mch.viewer.standalone;

import java.io.IOException;
import java.net.Socket;

public class PlayerConnection {
    private final Socket socket;
    private final Thread thread;
    private final PacketInputStream input;
    private final PacketOutputStream output;
    private final ServerboundPackets incomingPackets;
    private final ClientboundPackets outgoingPackets;
    private ProtocolState state = ProtocolState.HANDSHAKE;

    public PlayerConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new PacketInputStream(socket.getInputStream());
        this.output = new PacketOutputStream(socket.getOutputStream());
        this.incomingPackets = new ServerboundPackets(this, this.input);
        this.outgoingPackets = new ClientboundPackets(this, this.output);
        this.thread = new Thread(() -> {
            try {
                while (true) {
                    this.handleInput();
                }
            } catch (IOException e) {
                System.out.println("PlayerConnection closed: " + socket.getInetAddress() + ":" + socket.getPort());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        this.thread.start();
    }

    private void handleInput() throws IOException {
        int packetLength = this.input.readVarInt();
        int packetId = this.input.readVarInt();
        System.out.println("Received packet with ID: " + packetId + " and length: " + packetLength);
        int contentLength = packetLength - this.input.byteSizeOfVarInt(packetId);
        if (!this.incomingPackets.handlePacket(packetId, contentLength)) {
            System.out.println("Unknown packet ID: " + packetId);
            this.input.skipNBytes(contentLength);
        }
    }

    public ProtocolState getState() {
        return this.state;
    }

    public void setState(ProtocolState protocolState) {
        this.state = protocolState;
        System.out.println("The connection is now in the " + protocolState + " state.");
    }

    public ServerboundPackets getIncomingPackets() {
        return this.incomingPackets;
    }

    public ClientboundPackets getOutgoingPackets() {
        return this.outgoingPackets;
    }
}
