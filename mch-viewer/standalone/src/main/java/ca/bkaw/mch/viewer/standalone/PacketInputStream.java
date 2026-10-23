package ca.bkaw.mch.viewer.standalone;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PacketInputStream extends DataInputStream {
    public PacketInputStream(InputStream in) {
        super(in);
    }

    public int readVarInt() throws IOException {
        int value = 0;

        for (int position = 0; position < 32; position += 7) {
            byte currentByte = this.readByte();
            value |= (currentByte & 0x7F) << position;

            if ((currentByte & 0x80) == 0) {
                return value;
            }
        }
        throw new IOException("VarInt is too big");
    }

    public int byteSizeOfVarInt(int value) {
        int size = 0;
        do {
            value >>>= 7;
            size++;
        } while (value != 0);
        return size;
    }

    public String readString() throws IOException {
        int length = this.readVarInt();
        byte[] bytes = new byte[length];
        this.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public UUID readUUID() throws IOException {
        long mostSigBits = this.readLong();
        long leastSigBits = this.readLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    public int byteSizeOfString(String channel) {
        int length = channel.getBytes(StandardCharsets.UTF_8).length;
        return this.byteSizeOfVarInt(length) + length;
    }
}
