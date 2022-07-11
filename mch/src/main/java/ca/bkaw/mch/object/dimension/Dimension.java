package ca.bkaw.mch.object.dimension;

import ca.bkaw.mch.object.CatUtil;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.SerializationUtil;
import ca.bkaw.mch.object.StorageObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class Dimension extends StorageObject {
    public static final String OVERWORLD = "overworld";
    public static final String NETHER = "nether";
    public static final String THE_END = "the_end";

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
}
