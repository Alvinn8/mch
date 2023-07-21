package ca.bkaw.mch.object.worldcontainer;

import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.world.World;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorldContainer extends StorageObject {
    private final Map<String, Reference20<World>> worlds;

    public WorldContainer(DataInput dataInput) throws IOException {
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        int size = dataInput.readInt();
        this.worlds = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String worldIdentifier = dataInput.readUTF();
            Reference20<World> worldReference = Reference20.read(dataInput, ObjectStorageTypes.WORLD);
            this.worlds.put(worldIdentifier, worldReference);
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.worlds.size());
        for (Map.Entry<String, Reference20<World>> entry : this.worlds.entrySet()) {
            dataOutput.writeUTF(entry.getKey());
            entry.getValue().write(dataOutput);
        }
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder("world container:");
        for (Map.Entry<String, Reference20<World>> entry : this.worlds.entrySet()) {
            str.append(entry.getKey());
            str.append(": ");
            str.append(entry.getValue().getSha1().asHex());
        }
        return str.toString();
    }
}
