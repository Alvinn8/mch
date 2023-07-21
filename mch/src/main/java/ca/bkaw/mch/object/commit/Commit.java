package ca.bkaw.mch.object.commit;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;

public class Commit extends StorageObject {
    private final String message;
    private final long time;
    private final Reference20<WorldContainer> worldContainer;

    public Commit(String message, long time, Reference20<WorldContainer> worldContainer) {
        this.message = message;
        this.time = time;
        this.worldContainer = worldContainer;
    }

    public Commit(DataInput dataInput) throws IOException {
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        this.message = dataInput.readUTF();
        this.time = dataInput.readLong();
        this.worldContainer = Reference20.read(dataInput, ObjectStorageTypes.WORLD_CONTAINER);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeUTF(this.message);
        dataOutput.writeLong(this.time);
        this.worldContainer.write(dataOutput);
    }

    @Override
    public String cat() {
        return "message: " +
            this.message +
            "\ntime: " +
            new Date(this.time) +
            "\nworld container: " +
            this.worldContainer.getSha1().asHex();
    }
}
