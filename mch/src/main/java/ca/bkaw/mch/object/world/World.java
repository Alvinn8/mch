package ca.bkaw.mch.object.world;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A storage object that stores a specific version of a world.
 */
public class World extends StorageObject {
    public static final int MAGIC = FileMagic.WORLD;

    private final Layout layout;
    private final @Nullable Reference20<Tree> miscellaneousFiles;
    private final Map<String, Reference20<Dimension>> dimensions;

    /**
     * The layout that determines how dimensions are stored in the world.
     * <p>
     * This is used when restoring a world to determine how to restore the dimensions.
     * The dimensions are still stored in the same way regardless in the mch repository.
     */
    public enum Layout {
        /**
         * Nether is placed in "DIM-1" and the end is placed in "DIM1". The overworld is
         * placed in the root of the world folder. This is the layout used by Minecraft
         * versions before 26.0.
         */
        CLASSIC(1),
        /**
         * All dimensions are placed in the "dimensions" folder. This is the layout used by
         * Minecraft versions 26.1 and later.
         */
        DIMENSIONS(2),
        ;

        private final int value;
        Layout(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static Layout fromValue(int value) throws IOException {
            for (Layout layout : Layout.values()) {
                if (layout.getValue() == value) {
                    return layout;
                }
            }
            throw new IOException("Invalid world layout version: " + value);
        }
    }

    public World(DataInput dataInput) throws IOException {
        FileMagic.validate(dataInput, MAGIC);
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 2);
        if (mchVersion > 13) {
            this.layout = Layout.fromValue(dataInput.readByte());
            boolean hasMiscellaneousFiles = dataInput.readBoolean();
            this.miscellaneousFiles = hasMiscellaneousFiles ? Reference20.read(dataInput, ObjectStorageTypes.TREE) : null;
        } else {
            this.layout = Layout.CLASSIC;
            this.miscellaneousFiles = null;
        }
        int size = dataInput.readInt();
        this.dimensions = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String dimensionKey = dataInput.readUTF();
            Reference20<Dimension> dimensionReference = Reference20.read(dataInput, ObjectStorageTypes.DIMENSION);
            this.dimensions.put(dimensionKey, dimensionReference);
        }
    }

    public World(Layout layout,  Reference20<Tree> miscellaneousFiles) {
        this.layout = layout;
        this.miscellaneousFiles = miscellaneousFiles;
        this.dimensions = new HashMap<>();
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeByte(this.layout.getValue());
        dataOutput.writeBoolean(this.miscellaneousFiles != null);
        if (this.miscellaneousFiles != null) {
            this.miscellaneousFiles.write(dataOutput);
        }
        dataOutput.writeInt(this.dimensions.size());
        for (Map.Entry<String, Reference20<Dimension>> entry : this.dimensions.entrySet()) {
            dataOutput.writeUTF(entry.getKey());
            entry.getValue().write(dataOutput);
        }
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder();
        str.append("layout: ");
        str.append(this.layout.name());
        str.append("\n");
        str.append("miscellaneous files: ");
        str.append(this.miscellaneousFiles != null ? this.miscellaneousFiles.getSha1().asHex() : "(none)");
        str.append("\n");
        for (Map.Entry<String, Reference20<Dimension>> entry : this.dimensions.entrySet()) {
            str.append(entry.getKey());
            str.append(": ");
            str.append(entry.getValue().getSha1().asHex());
            str.append("\n");
        }
        return str.toString();
    }

    public void addDimension(String dimensionKey, Reference20<Dimension> dimensionReference) {
        this.dimensions.put(dimensionKey, dimensionReference);
    }

    @Nullable
    public Reference20<Dimension> getDimension(String dimensionKey) {
        return this.dimensions.get(dimensionKey);
    }

    /**
     * Get an unmodifiable view of the map of dimensions.
     *
     * @return The map.
     */
    public Map<String, Reference20<Dimension>> getDimensions() {
        return Collections.unmodifiableMap(this.dimensions);
    }

    public Layout getLayout() {
        return this.layout;
    }

    public @Nullable Reference20<Tree> getMiscellaneousFiles() {
        return this.miscellaneousFiles;
    }
}
