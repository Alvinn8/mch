package ca.bkaw.mch.object.chunk;

import ca.bkaw.mch.object.StorageObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// todo
public class Chunk extends StorageObject {
    public Chunk(DataInputStream stream) {
    }

    @Override
    public void serialize(DataOutputStream stream) {
    }

    @Override
    public String cat() {
        return null;
    }
}
