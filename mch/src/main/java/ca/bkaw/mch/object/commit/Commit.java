package ca.bkaw.mch.object.commit;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;

public class Commit extends StorageObject {
    private final String message;
    private final long time;
    private final Reference20<WorldContainer> worldContainer;
    private final Reference20<Commit> previousCommit;

    public Commit(String message, long time, Reference20<WorldContainer> worldContainer, @Nullable Reference20<Commit> previousCommit) {
        this.message = message == null ? "" : message;
        this.time = time;
        this.worldContainer = worldContainer;
        this.previousCommit = previousCommit;
    }

    public Commit(DataInput dataInput) throws IOException {
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        this.message = dataInput.readUTF();
        this.time = dataInput.readLong();
        this.worldContainer = Reference20.read(dataInput, ObjectStorageTypes.WORLD_CONTAINER);
        boolean hasPreviousCommit = dataInput.readBoolean();
        this.previousCommit = hasPreviousCommit ? Reference20.read(dataInput, ObjectStorageTypes.COMMIT) : null;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeUTF(this.message);
        dataOutput.writeLong(this.time);
        this.worldContainer.write(dataOutput);
        if (this.previousCommit == null) {
            dataOutput.writeBoolean(false);
        } else {
            dataOutput.writeBoolean(true);
            this.previousCommit.write(dataOutput);
        }
    }

    @Override
    public String cat() {
        return "message: " +
            this.message +
            "\ntime: " +
            new Date(this.time) +
            "\nworld container: " +
            this.worldContainer.getSha1().asHex() +
            "\nprevious commit: " +
            (this.previousCommit == null ? "null" : this.previousCommit.getSha1().asHex());
    }

    public Reference20<WorldContainer> getWorldContainer() {
        return this.worldContainer;
    }

    @Nullable
    public Reference20<Commit> getPreviousCommit() {
        return this.previousCommit;
    }

    @NotNull
    public String getMessage() {
        return this.message;
    }

    public long getTime() {
        return this.time;
    }
}
