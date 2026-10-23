package ca.bkaw.mch.viewer.standalone;

import ca.bkaw.mch.nbt.NbtByte;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtList;
import ca.bkaw.mch.nbt.NbtString;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientboundPackets {
    private final PlayerConnection connection;
    private final PacketOutputStream output;

    public ClientboundPackets(PlayerConnection connection, PacketOutputStream output) {
        this.connection = connection;
        this.output = output;
    }

    @FunctionalInterface
    private interface PacketWriter {
        void write(PacketOutputStream out) throws IOException;
    }

    private synchronized void sendPacket(int packetId, PacketWriter writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PacketOutputStream buffer = new PacketOutputStream(bytes);
        buffer.writeVarInt(packetId);
        writer.write(buffer);

        this.output.writeVarInt(bytes.size());
        bytes.writeTo(this.output);
        this.output.flush();
    }

    public void sendStatusResponse() throws IOException {
        JSONObject json = new JSONObject();
        JSONObject version = new JSONObject();
        version.put("name", "26.3");
        version.put("protocol", 777);
        json.put("version", version);
        JSONObject players = new JSONObject();
        players.put("max", 100);
        players.put("online", 0);
        json.put("players", players);
        JSONObject description = new JSONObject();
        description.put("text", "MCH Viewer");
        json.put("description", description);
        this.sendPacket(0x00, out -> out.writeString(json.toString()));
    }

    public void sendStatusPong(long payload) throws IOException {
        this.sendPacket(0x01, out -> out.writeLong(payload));
    }

    public void sendLoginSuccess(UUID uuid, String username) throws IOException {
        this.sendPacket(0x02, out -> {
            // Game profile
            out.writeUUID(uuid);
            out.writeString(username);
            out.writeVarInt(0); // Length of the Properties array
            // Session ID
            UUID sessionId = UUID.randomUUID();
            out.writeUUID(sessionId);
        });
    }

    public void sendFinishConfiguration() throws IOException {
        this.sendPacket(0x03, _ -> {
            // No content
        });
    }

    public void sendShowDialog() throws IOException {
        this.sendPacket(0x13, out -> {
            NbtCompound nbt = new NbtCompound();
            nbt.set("type", new NbtString("minecraft:multi_action"));
            nbt.set("title", new NbtString("MCH Viewer"));
            nbt.set("can_close_with_escape", new NbtByte((byte) 0));
            List<NbtCompound> actions = new ArrayList<>();
            NbtCompound action = new NbtCompound();
            action.set("label", new NbtString("Hello, world!"));
            actions.add(action);
            nbt.set("actions", new NbtList(NbtCompound.ID, actions.toArray(NbtCompound[]::new)));
            out.writeNbt(nbt);
        });
    }

}
