package ca.bkaw.mch.object.worldcontainer;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorldContainer extends StorageObject {
    public static final int MAGIC = FileMagic.WORLD_CONTAINER;

    private final Map<Sha1, Reference20<World>> worlds;

    public WorldContainer() {
        this.worlds = new HashMap<>();
    }

    public WorldContainer(DataInput dataInput) throws IOException {
        FileMagic.validate(dataInput, MAGIC);
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        int size = dataInput.readInt();
        this.worlds = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            Sha1 worldIdentifier = Sha1.read(dataInput);
            Reference20<World> worldReference = Reference20.read(dataInput, ObjectStorageTypes.WORLD);
            this.worlds.put(worldIdentifier, worldReference);
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.worlds.size());
        for (Map.Entry<Sha1, Reference20<World>> entry : this.worlds.entrySet()) {
            dataOutput.write(entry.getKey().getBytes());
            entry.getValue().write(dataOutput);
        }
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<Sha1, Reference20<World>> entry : this.worlds.entrySet()) {
            str.append(entry.getKey().asHex());
            str.append(": ");
            str.append(entry.getValue().getSha1().asHex());
            str.append("\n");
        }
        return str.toString();
    }

    public void addWorld(Sha1 worldId, Reference20<World> worldReference) {
        this.worlds.put(worldId, worldReference);
    }

    @Nullable
    public Reference20<World> getWorld(Sha1 id) {
        return this.worlds.get(id);
    }

    public Map<Sha1, Reference20<World>> getWorlds() {
        return this.worlds;
    }
}
