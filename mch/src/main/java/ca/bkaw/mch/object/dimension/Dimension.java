package ca.bkaw.mch.object.dimension;

import ca.bkaw.mch.object.CatUtil;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.SerializationUtil;
import ca.bkaw.mch.object.StorageObject;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class Dimension extends StorageObject {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String THE_END = "minecraft:the_end";

    private final Map<String, Reference20> regionFiles;

    public Dimension(Map<String, Reference20> regionFiles) {
        this.regionFiles = regionFiles;
    }

    public Dimension(DataInputStream stream) throws IOException {
        this.regionFiles = SerializationUtil.readMap(stream);
    }

    @Override
    public void serialize(DataOutputStream stream) throws IOException {
        SerializationUtil.writeMap(this.regionFiles, stream);
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder("region files:\n");
        CatUtil.printMap(this.regionFiles, str);
        return str.toString();
    }

    public static class RegionFileReference {
        private final int versionNumber;
        private final long lastModifiedTime;

        public RegionFileReference(DataInput dataInput) throws IOException {
            this.versionNumber = dataInput.readInt();
            this.lastModifiedTime = dataInput.readLong();
        }

        public RegionFileReference(int versionNumber, long lastModifiedTime) {
            this.versionNumber = versionNumber;
            this.lastModifiedTime = lastModifiedTime;
        }

        public void write(DataOutput dataOutput) throws IOException {
            dataOutput.writeInt(this.versionNumber);
            dataOutput.writeLong(this.lastModifiedTime);
        }
    }
}
