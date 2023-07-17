package ca.bkaw.mch.test;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.nbt.NbtCompound;
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
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Objects;

public class TestMain {
    public static void main(String[] args) throws IOException {
        PrintStream stream = new PrintStream(new BufferedOutputStream(new FileOutputStream("mch-compare.txt")));

        FileSystem zip1 = openZip(Path.of("backup.zip"));
        FileSystem zip2 = openZip(Path.of("backup2.zip"));

        int totalRegionFilesCount = 0;
        int modifiedRegionFilesCount = 0;
        int unmodifiedRegionFilesCount = 0;

        for (Path region1Path : Files.list(zip1.getPath("svcraftx/region")).filter(path -> path.toString().endsWith(".mca")).toList()) {
            long start = System.currentTimeMillis();
            totalRegionFilesCount++;
            Path region2Path = zip2.getPath("svcraftx/region", region1Path.getFileName().toString());

            BasicFileAttributes attributes1 = Files.readAttributes(region1Path, BasicFileAttributes.class);
            BasicFileAttributes attributes2 = Files.readAttributes(region2Path, BasicFileAttributes.class);

            if (attributes1.lastModifiedTime().equals(attributes2.lastModifiedTime()) && attributes1.size() == attributes2.size()) {
                stream.println("=== REGION FILE NOT MODIFIED " + region1Path.getFileName());
                unmodifiedRegionFilesCount++;
                continue;
            }
            modifiedRegionFilesCount++;

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
            int blockEntityChangingChunks = 0;
            int sectionsButNotBlockEntityChangingChunks = 0;
            int blockEntityButNotSectionsChangingChunks = 0;

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

                    nbt1.remove("LastUpdate");
                    nbt2.remove("LastUpdate");
                    nbt1.remove("InhabitedTime");
                    nbt2.remove("InhabitedTime");

                    if (nbt1.equals(nbt2)) {
                        if (!superEqual) {
                            stream.println("CHUNK EQUAL " + x + " " + z);
                        }
                        stream.println(formatBytes(nbt1.byteSize()));
                        equalChunkCount++;
                    } else {
                        stream.println("CHUNK DIFF " + x + " " + z);
                        diffingChunkCount++;

                        boolean sectionChange = Objects.equals(nbt1.get("Sections"), nbt2.get("Sections"));
                        if (sectionChange) {
                            stream.println("section changing chunk");
                            sectionChangingChunks++;
                        } else {
                            stream.println("has changes but not to sections");
                        }

                        boolean blockEntityChange = Objects.equals(nbt1.get("block_entities"), nbt2.get("block_entities"));
                        if (blockEntityChange) {
                            stream.println("block entity changing chunk");
                            blockEntityChangingChunks++;
                        } else {
                            stream.println("has changes but not to block entities");
                        }

                        if (blockEntityChange && !sectionChange) {
                            blockEntityButNotSectionsChangingChunks++;
                        }
                        if (sectionChange && !blockEntityChange) {
                            sectionsButNotBlockEntityChangingChunks++;
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
            printPercentage(stream, "blockEntityChangingChunks", blockEntityChangingChunks, diffingChunkCount);
            printPercentage(stream, "sectionsButNotBlockEntityChangingChunks", sectionsButNotBlockEntityChangingChunks, diffingChunkCount);
            printPercentage(stream, "blockEntityButNotSectionsChangingChunks", blockEntityButNotSectionsChangingChunks, diffingChunkCount);

            long end = System.currentTimeMillis();
            long diff = end - start;

            printPercentage(System.out, "emptyChunkCount", emptyChunkCount, totalChunkCount);
            System.out.println("Done " + diff + " ms");
            stream.println("Done " + diff + " ms");;

            // (?<!LastUpdate: |InhabitedTime: )DIFF
        }

        stream.println("\n\n==== OVERALL SUMMARY");
        printPercentage(stream, "modifiedRegionFilesCount", modifiedRegionFilesCount, totalRegionFilesCount);
        printPercentage(stream, "unmodifiedRegionFilesCount", unmodifiedRegionFilesCount, totalRegionFilesCount);
        stream.close();
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

    private static FileSystem openZip(Path zipFilePath) throws IOException {
        return FileSystems.newFileSystem(URI.create("jar:" + zipFilePath.toUri()), new HashMap<>());
    }

    public static void main2(String[] args) throws IOException {
        MchRepository repository = new MchRepository(Path.of("."));

        HashMap<String, Reference20> dimensions = new HashMap<>();
        dimensions.put("overworld", new Reference20(new Sha1(new byte[20])));

        Commit commit = new Commit("Test commit", System.currentTimeMillis(), dimensions);

        ObjectStorageTypes.COMMIT.save(commit, repository);
    }
}
