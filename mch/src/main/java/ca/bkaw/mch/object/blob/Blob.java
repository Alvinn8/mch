package ca.bkaw.mch.object.blob;

import ca.bkaw.mch.object.StorageObject;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

public class Blob extends StorageObject {
    private final byte[] bytes;

    public Blob(byte[] bytes) {
        this.bytes = bytes;
    }

    public Blob(DataInputStream dataInput) throws IOException {
        this.bytes = dataInput.readAllBytes();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.write(this.bytes);
    }

    @Override
    public String cat() {
        return this.bytes.length + " bytes";
    }
}
