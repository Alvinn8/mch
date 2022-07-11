package ca.bkaw.mch.object.commit;

import ca.bkaw.mch.object.CatUtil;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.SerializationUtil;
import ca.bkaw.mch.object.StorageObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class Commit extends StorageObject {
    private final String message;
    private final long time;
    private final Map<String, Reference20> dimensions;

    public Commit(String message, long time, Map<String, Reference20> dimensions) {
        this.message = message;
        this.time = time;
        this.dimensions = dimensions;
    }

    public Commit(DataInputStream stream) throws IOException {
        this.message = stream.readUTF();
        this.time = stream.readLong();
        this.dimensions = SerializationUtil.readMap(stream);
    }

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeUTF(this.message);
        stream.writeLong(this.time);
        SerializationUtil.writeMap(this.dimensions, stream);
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder(
            "message: " + this.message
            + "\ntime: " + new Date(this.time)
            + "\ndimensions:\n"
        );
        CatUtil.printMap(this.dimensions, str);
        return str.toString();
    }
}
