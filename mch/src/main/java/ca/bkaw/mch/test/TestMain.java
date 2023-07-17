package ca.bkaw.mch.test;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtList;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.region.McRegionFile;
import ca.bkaw.mch.repository.MchRepository;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.HashMap;

public class TestMain {
    public static void main4(String[] args) throws IOException {
        PrintStream stream = new PrintStream(new FileOutputStream("research-data/compare.txt"));

        McRegionFile region1 = new McRegionFile(Path.of("research-data/svcraft7/2021-6-28/world/region/r.0.0.mca"));

        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                NbtCompound nbt1 = NbtTag.readCompound(region1.readChunk(x, z));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        PrintStream stream = new PrintStream(new BufferedOutputStream(new FileOutputStream("research-data/compare.txt")));

        for (Path region1Path : Files.list(Path.of("research-data/svcraft7/2021-6-28/world/region")).filter(path -> path.toString().endsWith(".mca")).toList()) {
            long start = System.currentTimeMillis();
            Path region2Path = Path.of("research-data/svcraft7/2021-6-29/world/region", region1Path.getFileName().toString());

            McRegionFile region1 = new McRegionFile(region1Path);
            McRegionFile region2 = new McRegionFile(region2Path);

            stream.println("=== REGION " + region1Path.getFileName());
            System.out.println("Scanning " + region1Path.getFileName());

            int totalChunkCount = 0;
            int emptyChunkCount = 0;
            int nonEmptyChunkCount = 0;
            int superEqualChunkCount = 0;
            int equalChunkCount = 0;
            int diffingChunkCount = 0;
            int sectionChangingChunks = 0;

            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    totalChunkCount++;
                    if (!region1.hasChunk(x, z) || !region2.hasChunk(x, z)) {
                        stream.println("CHUNK _EMPTY_ " + x + " " + z);
                        emptyChunkCount++;
                        continue;
                    }
                    nonEmptyChunkCount++;
                    NbtCompound nbt1 = NbtTag.readCompound(region1.readChunk(x, z));
                    NbtCompound nbt2 = NbtTag.readCompound(region2.readChunk(x, z));

                    boolean superEqual = nbt1.equals(nbt2);
                    if (superEqual) {
                        stream.println("CHUNK EQUAL SUPER");
                        superEqualChunkCount++;
                    }

                    nbt1.getCompound("Level").remove("LastUpdate");
                    nbt2.getCompound("Level").remove("LastUpdate");
                    nbt1.getCompound("Level").remove("InhabitedTime");
                    nbt2.getCompound("Level").remove("InhabitedTime");

                    if (nbt1.equals(nbt2)) {
                        if (!superEqual) {
                            stream.println("CHUNK EQUAL " + x + " " + z);
                        }
                        stream.println(formatBytes(nbt1.byteSize()));
                        equalChunkCount++;
                    } else {
                        stream.println("CHUNK DIFF " + x + " " + z);
                        diffingChunkCount++;

                        NbtList sections1 = (NbtList) nbt1.getCompound("Level").get("Sections");
                        NbtList sections2 = (NbtList) nbt2.getCompound("Level").get("Sections");
                        if ((sections1 == sections2) || (sections1 != null && sections2 != null && !sections1.equals(sections2))) {
                            stream.println("section changing chunk");
                            sectionChangingChunks++;
                        } else {
                            stream.println("has changes but not to sections");
                        }

                        stream.println(nbt1.createCompareReport(nbt2));
                    }

                    // String str = nbt1.createCompareReport(nbt2);
                    // stream.println("==== CHUNK " + x + " " + z);
                    // stream.println(str.length() + " characters");
                    // stream.println(str);
                }
            }

            stream.println("=== REGION SUMMARY " + region1Path.getFileName());
            printPercentage(stream, "emptyChunkCount", emptyChunkCount, totalChunkCount);
            printPercentage(stream, "nonEmptyChunkCount", nonEmptyChunkCount, totalChunkCount);
            printPercentage(stream, "superEqualChunkCount", superEqualChunkCount, totalChunkCount);
            printPercentage(stream, "equalChunkCount", equalChunkCount, totalChunkCount);
            printPercentage(stream, "diffingChunkCount", diffingChunkCount, totalChunkCount);
            printPercentage(stream, "sectionChangingChunks", sectionChangingChunks, totalChunkCount);
            printPercentage(stream, "sectionChangingChunks", sectionChangingChunks, diffingChunkCount);

            long end = System.currentTimeMillis();
            long diff = end - start;

            printPercentage(System.out, "emptyChunkCount", emptyChunkCount, totalChunkCount);
            System.out.println("Done " + diff + " ms");
            stream.println("Done " + diff + " ms");;

            // (?<!LastUpdate: |InhabitedTime: )DIFF
        }
    }

    private static void printPercentage(PrintStream stream, String name, int count, int total) {
        String percentage = total == 0 ? "N/A" : (int) (((double) count / total) * 100) + "%";
        stream.println(name + " " + count + "\t/\t" + total + "\t(" + percentage + ")");
    }

    public static String formatBytes(long bytes) {
        final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.##");
        if (bytes > 1E9) {
            return DECIMAL_FORMAT.format(((double) bytes / 10E9)) + " GB";
        }
        if (bytes > 1E6) {
            return DECIMAL_FORMAT.format(((double) bytes / 10E6)) + " MB";
        }
        if (bytes > 1E3) {
            return DECIMAL_FORMAT.format(((double) bytes / 10E3)) + " kB";
        }
        return bytes + " bytes";
    }

    public static void main2(String[] args) throws IOException {
        MchRepository repository = new MchRepository(Path.of("."));

        HashMap<String, Reference20> dimensions = new HashMap<>();
        dimensions.put("overworld", new Reference20(new Sha1(new byte[20])));

        Commit commit = new Commit("Test commit", System.currentTimeMillis(), dimensions);

        ObjectStorageTypes.COMMIT.save(commit, repository);
    }
}
