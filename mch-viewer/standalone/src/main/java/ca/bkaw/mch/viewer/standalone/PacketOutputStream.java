package ca.bkaw.mch.viewer.standalone;

import ca.bkaw.mch.nbt.NbtTag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PacketOutputStream extends DataOutputStream {
    public PacketOutputStream(OutputStream out) {
        super(out);
    }

    public void writeVarInt(int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            this.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        this.writeByte(value & 0x7F);
    }

    public void writeString(String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        this.writeVarInt(bytes.length);
        this.write(bytes);
    }

    public void writeUUID(UUID sessionId) throws IOException {
        this.writeLong(sessionId.getMostSignificantBits());
        this.writeLong(sessionId.getLeastSignificantBits());

    }

    public void writeNbt(NbtTag tag) throws IOException {
        this.writeByte(tag.getId());
        tag.write(this);
    }
}
