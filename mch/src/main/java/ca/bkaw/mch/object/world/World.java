package ca.bkaw.mch.object.world;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.dimension.Dimension;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A storage object that stores a specific version of a world.
 */
public class World extends StorageObject {
    private final Map<String, Reference20<Dimension>> dimensions;

    public World(DataInput dataInput) throws IOException {
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        int size = dataInput.readInt();
        this.dimensions = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String dimensionKey = dataInput.readUTF();
            Reference20<Dimension> dimensionReference = Reference20.read(dataInput, ObjectStorageTypes.DIMENSION);
            this.dimensions.put(dimensionKey, dimensionReference);
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.dimensions.size());
        for (Map.Entry<String, Reference20<Dimension>> entry : this.dimensions.entrySet()) {
            dataOutput.writeUTF(entry.getKey());
            entry.getValue().write(dataOutput);
        }
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder("world:\n");
        for (Map.Entry<String, Reference20<Dimension>> entry : this.dimensions.entrySet()) {
            str.append(entry.getKey());
            str.append(": ");
            str.append(entry.getValue().getSha1().asHex());
            str.append("\n");
        }
        return str.toString();
    }
}
