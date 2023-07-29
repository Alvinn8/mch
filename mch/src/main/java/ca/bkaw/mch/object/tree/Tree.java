package ca.bkaw.mch.object.tree;

import ca.bkaw.mch.FileMagic;
import ca.bkaw.mch.MchVersion;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.blob.Blob;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Tree extends StorageObject {
    public static final int MAGIC = FileMagic.TREE;

    private final Map<String, Reference20<Tree>> trees;
    private final Map<String, Reference20<Blob>> blobs;

    public Tree() {
        this.trees = new LinkedHashMap<>();
        this.blobs = new LinkedHashMap<>();
    }

    public Tree(DataInput dataInput) throws IOException {
        if (dataInput.readInt() != MAGIC) {
            throw new RuntimeException("Expected tree magic number. Is the tree corrupted?");
        }
        int mchVersion = dataInput.readInt();
        MchVersion.validate(mchVersion, 3);

        // Use LinkedHashMap to ensure writing the tree produces a consistent order.

        int treesSize = dataInput.readInt();
        this.trees = new LinkedHashMap<>(treesSize);
        for (int i = 0; i < treesSize; i++) {
            String directoryName = dataInput.readUTF();
            Reference20<Tree> treeReference = Reference20.read(dataInput, ObjectStorageTypes.TREE);

            this.trees.put(directoryName, treeReference);
        }

        int blobsSize = dataInput.readInt();
        this.blobs = new LinkedHashMap<>(blobsSize);
        for (int i = 0; i < blobsSize; i++) {
            String fileName = dataInput.readUTF();
            Reference20<Blob> blobReference = Reference20.read(dataInput, ObjectStorageTypes.BLOB);

            this.blobs.put(fileName, blobReference);
        }
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(MAGIC);
        dataOutput.writeInt(MchVersion.VERSION_NUMBER);
        dataOutput.writeInt(this.trees.size());
        for (Map.Entry<String, Reference20<Tree>> entry : this.trees.entrySet()) {
            dataOutput.writeUTF(entry.getKey());
            entry.getValue().write(dataOutput);
        }
        dataOutput.writeInt(this.blobs.size());
        for (Map.Entry<String, Reference20<Blob>> entry : this.blobs.entrySet()) {
            dataOutput.writeUTF(entry.getKey());
            entry.getValue().write(dataOutput);
        }
    }

    @Override
    public String cat() {
        StringBuilder str = new StringBuilder();
        if (!this.trees.isEmpty()) {
            str.append("directories:\n");
            for (Map.Entry<String, Reference20<Tree>> entry : this.trees.entrySet()) {
                str.append(entry.getKey());
                str.append(":\t");
                str.append(entry.getValue().getSha1().asHex());
                str.append('\n');
            }
        }
        if (!this.blobs.isEmpty()) {
            str.append("files:\n");
            for (Map.Entry<String, Reference20<Blob>> entry : this.blobs.entrySet()) {
                str.append(entry.getKey());
                str.append(":\t");
                str.append(entry.getValue().getSha1().asHex());
                str.append('\n');
            }
        }
        return str.toString();
    }

    public void addSubTree(String directoryName, Reference20<Tree> subTreeReference) {
        this.trees.put(directoryName, subTreeReference);
    }

    public void addFile(String fileName, Reference20<Blob> blobReference) {
        this.blobs.put(fileName, blobReference);
    }
}
