package ca.bkaw.mch.viewer.standalone;

import java.io.IOException;
import java.util.UUID;

public class ServerboundPackets {
    private final PlayerConnection connection;
    private final PacketInputStream input;

    public ServerboundPackets(PlayerConnection connection, PacketInputStream input) {
        this.connection = connection;
        this.input = input;
    }

    public boolean handlePacket(int packetId, int contentLength) throws IOException {
        // contentLength = length of packet contents, excluding the packet ID and length itself.
        boolean handled = switch (this.connection.getState()) {
            case HANDSHAKE -> this.handleHandshakePacket(packetId);
            case STATUS -> this.handleStatusPacket(packetId);
            case LOGIN -> this.handleLoginPacket(packetId);
            case CONFIGURATION -> this.handleConfigurationPacket(packetId, contentLength);
            default -> false;
        };
        if (!handled) {
            System.out.println("Unknown packet ID: " + packetId);
            return false;
        }
        return true;
    }

    private boolean handleHandshakePacket(int packetId) throws IOException {
        // Only has one packet
        if (packetId == 0x00) {
            this.handleHandshake();
            return true;
        }
        return false;
    }

    private boolean handleStatusPacket(int packetId) throws IOException {
        switch (packetId) {
            case 0x00 -> this.handleStatusRequest();
            case 0x01 -> this.handleStatusPing();
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean handleLoginPacket(int packetId) throws IOException {
        switch (packetId) {
            case 0x00 -> this.handleLoginStart();
            case 0x03 -> this.handleLoginAck();
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean handleConfigurationPacket(int packetId, int contentLength) throws IOException {
        switch (packetId) {
            case 0x00 -> this.handleClientInformation();
            case 0x02 -> this.handlePluginMessage(contentLength);
            default -> {
                return false;
            }
        }
        return true;
    }

    public void handleHandshake() throws IOException {
        int protocolVersion = this.input.readVarInt();
        String serverAddress = this.input.readString();
        int serverPort = this.input.readUnsignedShort();
        int intent = this.input.readVarInt();

        System.out.println("Handshake packet received:");
        System.out.println("Protocol Version: " + protocolVersion);
        System.out.println("Server Address: " + serverAddress);
        System.out.println("Server Port: " + serverPort);
        System.out.println("Intent: " + intent);

        // Update the connection state based on the next state
        if (intent == 1) {
            this.connection.setState(ProtocolState.STATUS);
        } else if (intent == 2) {
            this.connection.setState(ProtocolState.LOGIN);
        } else {
            System.out.println("Unknown next state: " + intent);
        }
    }

    private void handleStatusRequest() throws IOException {
        // No input. Reply with status.
        this.connection.getOutgoingPackets().sendStatusResponse();
    }

    private void handleStatusPing() throws IOException {
        long payload = this.input.readLong();
        this.connection.getOutgoingPackets().sendStatusPong(payload);
    }

    private void handleLoginStart() throws IOException {
        String username = this.input.readString();
        UUID uuid = this.input.readUUID();
        this.connection.getOutgoingPackets().sendLoginSuccess(uuid, username);
    }

    private void handleLoginAck() throws IOException {
        // No input. Change to configuration state.
        this.connection.setState(ProtocolState.CONFIGURATION);
    }

    private void handleClientInformation() throws IOException {
        String locale = this.input.readString();
        int viewDistance = this.input.readVarInt();
        int chatMode = this.input.readVarInt();
        boolean chatColors = this.input.readBoolean();
        int skinParts = this.input.readUnsignedByte();
        int mainHand = this.input.readVarInt();
        boolean enableTextFiltering = this.input.readBoolean();
        boolean allowServerListings = this.input.readBoolean();
        int particleStatus = this.input.readVarInt();

        System.out.println("Client Information packet received:");
        System.out.println("Language: " + locale);
        System.out.println("View Distance: " + viewDistance);
        System.out.println("Chat Mode: " + chatMode);
        System.out.println("Chat Colors: " + chatColors);
        System.out.println("Skin Parts: " + skinParts);
        System.out.println("Main Hand: " + mainHand);
        System.out.println("Enable Text Filtering: " + enableTextFiltering);
        System.out.println("Allow Server Listings: " + allowServerListings);
        System.out.println("Particle Status: " + particleStatus);

        this.connection.getOutgoingPackets().sendShowDialog();
    }

    private void handlePluginMessage(int contentLength) throws IOException {
        String channel = this.input.readString();
        if (channel.equals("minecraft:brand")) {
            String brand = this.input.readString();
            System.out.println("Client Brand: " + brand);
            return;
        }
        byte[] data = new byte[contentLength - this.input.byteSizeOfString(channel)];
        this.input.readFully(data);

        System.out.println("Plugin Message packet received:");
        System.out.println("Channel: " + channel);
        System.out.println("Data Length: " + data.length);
    }
}
