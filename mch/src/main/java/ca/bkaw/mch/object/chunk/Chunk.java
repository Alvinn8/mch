package ca.bkaw.mch.object.chunk;

import ca.bkaw.mch.object.StorageObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Chunk extends StorageObject {
    private final byte[] bytes;

    public Chunk(DataInputStream stream) throws IOException {
        int size = stream.readInt();
        this.bytes = new byte[size];
        stream.readFully(this.bytes);
    }

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeInt(this.bytes.length);
        stream.write(this.bytes);
    }

    @Override
    public String cat() {
        return "chunk with byte length: " + this.bytes.length;
    }
}
