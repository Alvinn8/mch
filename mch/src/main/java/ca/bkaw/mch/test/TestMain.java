package ca.bkaw.mch.test;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.region.McRegionFile;
import ca.bkaw.mch.repository.MchRepository;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;

public class TestMain {
    public static void main(String[] args) throws IOException {
        McRegionFile region1 = new McRegionFile(Path.of("research/svcraft7/2021-6-28/world/region/r.0.0.mca").toFile());
        McRegionFile region2 = new McRegionFile(Path.of("research/svcraft7/2021-6-29/world/region/r.0.0.mca").toFile());

        PrintStream stream = new PrintStream(new FileOutputStream("research/compare.txt"));

        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                NbtCompound nbt1 = NbtTag.readCompound(region1.readChunk(0, 0));
                NbtCompound nbt2 = NbtTag.readCompound(region2.readChunk(0, 0));

                ((NbtCompound) nbt1.get("Level")).remove("LastUpdate");
                ((NbtCompound) nbt2.get("Level")).remove("LastUpdate");
                ((NbtCompound) nbt1.get("Level")).remove("InhabitedTime");
                ((NbtCompound) nbt2.get("Level")).remove("InhabitedTime");

                if (nbt1.equals(nbt2)) {
                    stream.println("CHUNK EQUAL " + x + " " + z);
                } else {
                    stream.println("CHUNK DIFF " + x + " " + z);
                    stream.println(nbt1.createCompareReport(nbt2));
                }

                // String str = nbt1.createCompareReport(nbt2);
                // stream.println("==== CHUNK " + x + " " + z);
                // stream.println(str.length() + " characters");
                // stream.println(str);
            }
        }

        // (?<!LastUpdate: |InhabitedTime: )DIFF
    }

    public static void main2(String[] args) throws IOException {
        MchRepository repository = new MchRepository(Path.of("."));

        HashMap<String, Reference20> dimensions = new HashMap<>();
        dimensions.put("overworld", new Reference20(new Sha1(new byte[20])));

        Commit commit = new Commit("Test commit", System.currentTimeMillis(), dimensions);

        ObjectStorageTypes.COMMIT.save(commit, repository);
    }
}
