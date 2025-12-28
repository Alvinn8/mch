package ca.bkaw.mch.test;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.chunk.ChunkStorage;
import ca.bkaw.mch.chunk.RegionFileChunk;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtList;
import ca.bkaw.mch.nbt.NbtLong;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.object.ObjectStorageType;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.region.MchRegionFile;
import ca.bkaw.mch.region.RegionStorageVisitor;
import ca.bkaw.mch.region.mc.McRegionFileReader;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class TestMain {
    public static void main(String[] args) throws IOException {
        McRegionFileReader reader = new McRegionFileReader(Path.of("/Users/Alvin/Downloads/r.-1.1_entities.mca"));
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                if (!reader.hasChunk(i, j)) {
                    continue;
                }
                DataInputStream stream = reader.readChunk(i, j);
                NbtCompound nbt = NbtTag.readCompound(stream);
                System.out.println("nbt = " + nbt);
                System.exit(0);
                System.out.println("Chunk " + i + ", " + j + ":\t" + nbt.byteSize() + " bytes");
            }
        }
    }

    public static void main16(String[] args) throws IOException {
        // Repair a corrupted repo where mchrv files was accidentally deleted.
        //
        // The idea is to use the LastUpdate tag in chunks to reconstruct which commit
        // the specific chunk versions belong to. We can compare this time in ticks to
        // the world's time at a specific commit by reading the Time value from the
        // level.dat. So if we, for each commit, find the current time, then for each
        // chunk in the broken files we get the latest possible chunk version number
        // which has a LastUpdate as close to the world time as possible, but not above
        // it, we should be able to reproduce the chunk version number for that commit.
        // That is what is missing in this case, those chunk version numbers.
        //
        // One detail to note is the order in which files are saved by mch. The level.dat
        // will be saved before region files, which means that chunks in region files may
        // have a LastUpdate time that is slightly ahead of the Time in the level.dat. In
        // this case, commits were performed every 24 hours. Commits usually took, in the
        // worst case, slightly more than 2 hours to perform. We can therefore safely add
        // like 4 hours to the level.dat time and use that as a safe upper limit for the
        // time which chunks try to approach. There are cases where manual backups have
        // been performed to the automated script crashing, so in these cases it may be
        // less than 4 hours between commits, but it should hopefully be rare.


        record RegionPos(int regionX, int regionZ) {};
        List<RegionPos> missingMchRvFiles = List.of(
            new RegionPos(-1, -4),
            new RegionPos(-1, -5),
            new RegionPos(0, -4),
            new RegionPos(0, -5)
        );
        Path repoPath = Path.of("/Users/Alvin/Documents/mch/metacraft/metacraft-survival/mch").toAbsolutePath();
        String outputDimensionKey = "mch_repair_repo_test";

        MchRepository repo = new MchRepository(repoPath);
        repo.readConfiguration();

        TrackedWorld trackedWorld = repo.getConfiguration().getTrackedWorld(Sha1.fromString("d6cd92545d49add9a5157a6bc7647a59ea4b1c38"));
        String dimensionKey = Dimension.OVERWORLD;

        Files.createDirectories(repoPath.resolve("world").resolve(trackedWorld.getId().asHex()).resolve("dimensions").resolve(outputDimensionKey).resolve("region"));

        // First check that the mchrv file doesn't exist. We don't want to accidentally
        // overwrite any valid files.
        for (RegionPos regionPos : missingMchRvFiles) {
            boolean existsInOverworld;
            try {
                MchRegionFile.read(repo, trackedWorld, dimensionKey, regionPos.regionX, regionPos.regionZ, 1);
                // We didn't throw yet? It must exist.
                existsInOverworld = true;
            } catch (NoSuchFileException e) {
                existsInOverworld = false;
            }
            boolean existsInOutputDimension;
            try {
                MchRegionFile.read(repo, trackedWorld, outputDimensionKey, regionPos.regionX, regionPos.regionZ, 1);
                // We didn't throw yet? It must exist.
                existsInOutputDimension = true;
            } catch (NoSuchFileException e) {
                existsInOutputDimension = false;
            }
            if (existsInOverworld || existsInOutputDimension) {
                throw new RuntimeException("The mchrv file already exists! r." + regionPos.regionX + "." + regionPos.regionZ + ".mchrv.zst. existsInOverworld = " + existsInOverworld + ", existsInOutputDimension = " + existsInOutputDimension);
            }
        }

        List<Commit> commits = new ArrayList<>();

        Reference20<Commit> commitRef = repo.getHeadCommit();
        while (commitRef != null) {
            Commit commit = commitRef.resolve(repo);
            commits.add(commit);
            commitRef = commit.getPreviousCommit();
        }

        List<Commit> commitsReverse = new ArrayList<>(commits);
        Collections.reverse(commitsReverse);

        for (RegionPos regionPos : missingMchRvFiles) {
            System.out.println();
            System.out.println("Procession region file " + regionPos);
            System.out.println();
            int regionX = regionPos.regionX;
            int regionZ = regionPos.regionZ;

            record ChunkInfo(int chunkVersionNumber, long lastUpdate) {}

            System.out.println("Preparing chunk info");
            List<ChunkInfo>[] chunkInfoListArray = new List[1024]; // Note that this is an array of lists.

            RegionStorageVisitor.visitReadOnly(repo, trackedWorld, dimensionKey, regionX, regionZ, (chunk) -> {
                ChunkStorage chunkStorage = null;
                // Make RegionStorageVisitor.Chunk#chunkStorage public to compile
                // chunkStorage = chunk.chunkStorage;

                List<Integer> availableChunkVersionNumbers = null;
                // Make ChunkStorage#chunkVersions public to compile
                // availableChunkVersionNumbers = chunkStorage.chunkVersions.keySet().stream().sorted(Integer::compareTo).toList();

                List<ChunkInfo> chunkInfoList = new ArrayList<>();
                for (int chunkVersionNumber : availableChunkVersionNumbers) {
                    RegionFileChunk chunkData = chunk.restore(chunkVersionNumber);
                    NbtCompound chunkNbt = chunkData.nbt();
                    long lastUpdate = ((NbtLong) chunkNbt.get("LastUpdate")).getValue();
                    chunkInfoList.add(new ChunkInfo(chunkVersionNumber, lastUpdate));
                }
                chunkInfoListArray[chunk.getIndex()] = chunkInfoList;
            });

            // Loop from first to the newest commit.
            for (Commit commit : commitsReverse) {
                System.out.println("Processing commit " + new Date(commit.getTime()));
                WorldContainer worldContainer = commit.getWorldContainer().resolve(repo);
                World world = worldContainer.getWorld(trackedWorld.getId()).resolve(repo);
                Dimension dimension = world.getDimension(dimensionKey).resolve(repo);

                Tree miscFiles = dimension.getMiscellaneousFiles().resolve(repo);
                Blob levelDatBlob = miscFiles.getFiles().get("level.dat").reference().resolve(repo);
                byte[] levelDatBytes = levelDatBlob.getBytes();
                NbtCompound levelDatRoot = NbtTag.readCompound(new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(levelDatBytes))));
                NbtCompound levelDat = (NbtCompound) levelDatRoot.get("Data");

                // The current world time for this commit.
                final long paddingTime = 4 * 60 * 60 * 20; // Add 4 hours due to reasons above.
                long rawWorldTimeTicks = ((NbtLong) levelDat.get("Time")).getValue();
                long worldTimeTicks = rawWorldTimeTicks;

                System.out.println("    Commit has raw time: " + rawWorldTimeTicks);
                System.out.println("    Commit has time: " + worldTimeTicks);

                Dimension.RegionFileReference regionFileRef = dimension.getRegionFile(regionX, regionZ);
                int regionVersionNumber = regionFileRef.getVersionNumber();
                System.out.println("    regionVersionNumber = " + regionVersionNumber);
                System.out.println("    //<editor-fold desc=\"Chunks\">");

                int[] chunkVersionNumbers = new int[1024];

                for (int chunkIndex = 0; chunkIndex < 1024; chunkIndex++) {
                    List<ChunkInfo> chunkInfoList = chunkInfoListArray[chunkIndex];
                    ChunkInfo bestMatch = null;
                    long bestMatchDeltaTime = Long.MAX_VALUE;
                    ChunkInfo bestAlmostMatch = null;
                    long bestAlmostMatchDeltaTime = Long.MIN_VALUE;
                    for (ChunkInfo chunkInfo : chunkInfoList) {
                        long deltaTime = worldTimeTicks - chunkInfo.lastUpdate;
                        if (deltaTime + paddingTime >= 0 && deltaTime < bestMatchDeltaTime) {
                            bestMatchDeltaTime = deltaTime;
                            bestMatch = chunkInfo;
                        }
                        // if (deltaTime < 0 && deltaTime > bestAlmostMatchDeltaTime) {
                        //     bestAlmostMatchDeltaTime = deltaTime;
                        //     bestAlmostMatch = chunkInfo;
                        // }
                    }
                    if (bestMatch != null) {
                        System.out.println("\tc" + chunkIndex + " bm " + bestMatch.chunkVersionNumber + " dT " + bestMatchDeltaTime + " = " + (bestMatchDeltaTime < 0 ? "(-) " : "") + formatTicks((int) Math.abs(bestMatchDeltaTime)));
                        if (bestAlmostMatch != null && -bestAlmostMatchDeltaTime < 60 * 60 * 20) {
                            System.out.println("\tRunner-upper dT " + bestAlmostMatchDeltaTime + " = (-) " + formatTicks((int) -bestAlmostMatchDeltaTime));
                        }
                        chunkVersionNumbers[chunkIndex] = bestMatch.chunkVersionNumber;
                    } else {
                        System.out.println("\tc" + chunkIndex + " found no match");
                        chunkVersionNumbers[chunkIndex] = 0; // Chunk doesn't exist.
                    }
                }
                System.out.println("    //</editor-fold>");

                // Write to a custom dimension to avoid writing data to the live repo and
                // accidentally breaking stuff.
                System.out.println("Writing region file r." + regionX + "." + regionZ + " to custom output dimension.");
                int generatedRegionVersionNumber = MchRegionFile.store(repo, trackedWorld, outputDimensionKey, regionX, regionZ, chunkVersionNumbers);
                if (regionVersionNumber != generatedRegionVersionNumber) {
                    System.out.println("Region version numbers did not align. Expected: " + regionVersionNumber + " but generated " + generatedRegionVersionNumber);
                }
                while (generatedRegionVersionNumber < regionVersionNumber) {
                    System.out.println("Force storing.");
                    // To compile, create this method and make sure it always generates a new version number.
                    // generatedRegionVersionNumber = MchRegionFile.storeForceNewVersionNumber(repo, trackedWorld, outputDimensionKey, regionX, regionZ, chunkVersionNumbers);
                    generatedRegionVersionNumber++; // temp, remove if uncomment above
                    System.out.println("Force stored as version number " + generatedRegionVersionNumber);
                }
                if (generatedRegionVersionNumber > regionVersionNumber) {
                    System.out.println("FAILURE");
                }
            }
        }
    }

    private static String formatTicks(int ticks) {
        int seconds = ticks / 20;
        if (seconds <= 60) {
            return seconds + " seconds, " + (ticks % 20) + " ticks";
        }
        int minutes = seconds / 60;
        if (minutes <= 60) {
            return minutes + " minutes, " + (seconds % 60) + " seconds, " + (ticks % 20) + " ticks";
        }
        int hours = minutes / 60;
        if (hours <= 24) {
            return hours + " hours, " + (minutes % 60) + " minutes, " + (seconds % 60) + " seconds, " + (ticks % 20) + " ticks";
        }
        int days = hours / 24;
        return days + " days, " + (hours % 24) + " hours";
    }

    public static void main15(String[] args) {
        List<Integer> test = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        int index = test.indexOf(8);

        test.add(index, 9);
        System.out.println(test);
    }

    public static void main14(String[] args) {
        Deque<Integer> lifoQueue = new LinkedList<>();

        for (int i = 0; i < 50; i++) {
            lifoQueue.add(i);
            if (lifoQueue.size() > 10) {
                lifoQueue.remove();
            }
        }
        System.out.println(lifoQueue);
    }

    public static void main13(String[] args) {
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath();
        Path path = Path.of(".").toAbsolutePath();
        Path relative = home.relativize(path);
        System.out.println("relative.getName(0) = " + relative.getName(0));
    }

    public static void main12(String[] args) throws IOException {
        McRegionFileReader reader = new McRegionFileReader(Path.of("/Users/Alvin/Downloads/r.-2.-4.mca"));
        System.out.println("reader.hasChunk(27, 27) = " + reader.hasChunk(27, 27));
        System.out.println("reader.getChunkLastModified(27, 27) = " + reader.getChunkLastModified(27, 27));
        NbtCompound chunkNbt = reader.readChunkNbt(27, 27);
        System.out.println("chunkNbt = " + chunkNbt);
    }

    public static void main11(String[] args) throws Exception {
        FTPClient client = new FTPClient();
        client.connect("REDACTED", 21);

        Field f1 = FTP.class.getDeclaredField("_controlOutput_");
        f1.setAccessible(true);
        BufferedWriter actualWriter = (BufferedWriter) f1.get(client);
        f1.set(client, new SpyWriter(actualWriter));

        Field f2 = FTP.class.getDeclaredField("_controlInput_");
        f2.setAccessible(true);
        BufferedReader actualReader = (BufferedReader) f2.get(client);
        f2.set(client, new SpyReader(actualReader));

        boolean success = client.login("REDACTED", "REDACTED");
        System.out.println("success = " + success);

        client.enterLocalPassiveMode();

        // System.out.println("client.list() = " + client.list());
        client.changeWorkingDirectory("lobby");
        System.out.println("client.printWorkingDirectory() = " + client.printWorkingDirectory());
        System.out.println("client.listFiles() = " + Arrays.toString(client.listFiles()));
        System.out.println("client.listDirectories() = " + Arrays.toString(client.listDirectories()));
    }

    static class SpyWriter extends BufferedWriter {

        public SpyWriter(@NotNull Writer out) {
            super(out);
        }

        @Override
        public void write(@NotNull String str) throws IOException {
            super.write(str);
            if (str.endsWith("\n")) {
                str = str.substring(0, str.length() - 1);
            }
            System.out.println("FTP > " + str);
        }
    }

    static class SpyReader extends BufferedReader {

        public SpyReader(@NotNull Reader in) {
            super(in);
        }

        @Override
        public String readLine() throws IOException {
            String line = super.readLine();
            System.out.println("FTP < " + line);
            return line;
        }
    }

    public static void main10(String[] args) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{
            0x78, (byte) 0x9C
        });
        InflaterInputStream inStream = new InflaterInputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        inStream.transferTo(out);
        System.out.println(Arrays.toString(out.toByteArray()));
    }

    public static void main9(String[] args) throws IOException {
        if (args.length < 3) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter arguments: ");
            args = scanner.nextLine().split(" ");
        }
        if ("exit".equals(args[0])) {
            return;
        }
        String typeId = args[1];
        String hash = args[2];

        ObjectStorageType<?> type = ObjectStorageTypes.getType(typeId);

        if (type == null) {
            System.out.println(typeId + " is not an object storage type.");
            return;
        }

        if (hash.length() != 40) {
            System.out.println("<red>Please specify the 40-character-hexadecimal SHA-1 hash of the object.");
            return;
        }

        Sha1 sha1 = Sha1.fromString(hash);

        MchRepository repository = new MchRepository(Path.of("run/mch"));

        StorageObject storageObject;
        try {
            storageObject = type.read(sha1, repository);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        System.out.println(typeId + " " + sha1.asHex() + ":\n" + storageObject.cat());

        main10(new String[0]);
    }

    public static void main7(String[] args) {
        System.out.println(Sha1.randomSha1());
    }

    private static NbtCompound getChunkNbt(String regionFileName) throws IOException {
        Path regionFilePath = Path.of("run/region/" + regionFileName);
        try (McRegionFileReader regionFile = new McRegionFileReader(regionFilePath)) {
            DataInputStream stream = regionFile.readChunk(0, 0);
            return NbtTag.readCompound(stream);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends NbtTag> T copyNbt(T nbt) throws IOException {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(outBytes);
        NbtTag.writeTag(dataOutput, nbt);

        ByteArrayInputStream inBytes = new ByteArrayInputStream(outBytes.toByteArray());
        DataInputStream dataInput = new DataInputStream(inBytes);
        return (T) NbtTag.readTag(dataInput);
    }

    public static void main3(String[] args) throws IOException {
        PrintStream stream = new PrintStream(new BufferedOutputStream(new FileOutputStream("mch-compare.txt")));

        FileSystem zip1 = openZip(Path.of("backup.zip"));
        FileSystem zip2 = openZip(Path.of("backup2.zip"));

        int totalRegionFilesCount = 0;
        int modifiedRegionFilesCount = 0;
        int unmodifiedRegionFilesCount = 0;
        int totalChunksCount = 0;
        int totalSectionChangingChunksCount = 0;
        int totalSectionTags = 0;

        Map<Set<String>, Integer> changeCounts = new HashMap<>();
        Map<Integer, Integer> changingSectionsCounts = new HashMap<>();
        Map<Integer, Integer> sectionsListLengths = new HashMap<>();

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

            McRegionFileReader region1 = new McRegionFileReader(region1Path);
            McRegionFileReader region2 = new McRegionFileReader(region2Path);

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
                    totalChunksCount++;
                    NbtCompound nbt1 = NbtTag.readCompound(region1.readChunk(x, z));
                    NbtCompound nbt2 = NbtTag.readCompound(region2.readChunk(x, z));

                    Set<String> changingKeys = new HashSet<>();

                    for (Map.Entry<String, NbtTag> entry : nbt1.entrySet()) {
                        String key = entry.getKey();
                        NbtTag tag1 = entry.getValue();
                        NbtTag tag2 = nbt2.get(key);
                        if (tag1 != null && tag2 != null && !tag1.equals(tag2)) {
                            changingKeys.add(key);
                        }
                    }
                    changingKeys = Set.copyOf(changingKeys);

                    changeCounts.put(changingKeys, changeCounts.getOrDefault(changingKeys, 0) + 1);

                    NbtList sections1_ = (NbtList) nbt1.get("sections");
                    if (sections1_ != null) {
                        totalSectionTags++;
                        int size = sections1_.getValue().length;
                        sectionsListLengths.put(size, sectionsListLengths.getOrDefault(size, 0) + 1);
                    }

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

                        NbtList sections1 = (NbtList) nbt1.get("sections");
                        NbtList sections2 = (NbtList) nbt2.get("sections");
                        boolean sectionChange = !Objects.equals(sections1, sections2);
                        if (sectionChange) {
                            stream.println("section changing chunk");
                            sectionChangingChunks++;
                            totalSectionChangingChunksCount++;
                            if (sections1 != null && sections2 != null) {
                                NbtTag[] sections1Value = sections1.getValue();
                                NbtTag[] sections2Value = sections2.getValue();
                                int length = Math.max(sections1Value.length, sections2Value.length);
                                int changedSections = 0;
                                for (int i = 0; i < length; i++) {
                                    NbtTag section1 = i < sections1Value.length ? sections1Value[i] : null;
                                    NbtTag section2 = i < sections2Value.length ? sections2Value[i] : null;
                                    if (!Objects.equals(section1, section2)) {
                                        changedSections++;
                                    }
                                }
                                changingSectionsCounts.put(changedSections, changingSectionsCounts.getOrDefault(changedSections, 0) + 1);
                            }
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

                        stream.println(nbt1.createCompareReport(nbt2, ""));
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
        System.out.println();
        int finalTotalChunksCount = totalChunksCount;
        changeCounts.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).forEach(entry -> {
            printPercentage(stream, entry.getKey().toString(), entry.getValue(), finalTotalChunksCount);
        });
        System.out.println("amount of sections that change:");
        int finalTotalSectionChangingChunksCount = totalSectionChangingChunksCount;
        changingSectionsCounts.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).forEach(entry -> {
            printPercentage(stream, entry.getKey().toString(), entry.getValue(), finalTotalSectionChangingChunksCount);
        });
        System.out.println("length of sections tag:");
        int finalTotalSectionTags = totalSectionTags;
        sectionsListLengths.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).forEach(entry -> {
            printPercentage(stream, entry.getKey().toString(), entry.getValue(), finalTotalSectionTags);
        });
        stream.close();
    }

    private static void printPercentage(PrintStream stream, String name, int count, int total) {
        String percentage = total == 0 ? "N/A" : (int) (((double) count / total) * 100) + "%";
        stream.println(name + " " + count + "\t/\t" + total + "\t(" + percentage + ")");
    }

    public static String formatBytes(long bytes) {
        final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.##");
        if (bytes > 1E9) {
            return DECIMAL_FORMAT.format(((double) bytes / 1E9)) + " GB";
        }
        if (bytes > 1E6) {
            return DECIMAL_FORMAT.format(((double) bytes / 1E6)) + " MB";
        }
        if (bytes > 1E3) {
            return DECIMAL_FORMAT.format(((double) bytes / 1E3)) + " kB";
        }
        return bytes + " bytes";
    }

    private static FileSystem openZip(Path zipFilePath) throws IOException {
        return FileSystems.newFileSystem(URI.create("jar:" + zipFilePath.toUri()), new HashMap<>());
    }
}
