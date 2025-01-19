package ca.bkaw.mch.e2e;

import ca.bkaw.mch.cli.MchCli;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtLong;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.region.mc.McRegionFileReader;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class EndToEndTestUtils {
    public static void mchCli(Path workingDir, String... args) {
        int exitCode = MchCli.run(workingDir, args);
        assertEquals(
            0,
            exitCode,
            "mch " + String.join(" ", args) + " exited with exit code " + exitCode
        );
    }

    public static void extractWorld(String worldName, Path destPath) throws IOException {
        Files.createDirectories(destPath);
        Path zipPath = Path.of("src/test/resources/test-worlds/" + worldName + ".zip");

        try (FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), new HashMap<>())) {
            Path path = fileSystem.getPath(".");
            extractRecursive(path, destPath);
        }
    }

    private static void extractRecursive(Path sourceDir, Path destDir) throws IOException {
        try (Stream<Path> stream = Files.list(sourceDir)) {
            for (Path source : stream.toList()) {
                Path dest = destDir.resolve(source.getFileName().toString());
                if (Files.isRegularFile(source)) {
                    Files.copy(source, dest);
                } else if (Files.isDirectory(source)) {
                    Files.createDirectory(dest);
                    extractRecursive(source, dest);
                }
            }
        }
    }

    public static void assertWorldsEqualLossless(Path expected, Path actual) throws IOException {
        assertWorldsEqual(expected, actual, null);
    }

    public static void assertWorldsEqualLossy(Path expected, Path actual, int inhabitedTimeFilter) throws IOException {
        assertWorldsEqual(expected, actual, inhabitedTimeFilter);
    }

    private static void assertWorldsEqual(Path expected, Path actual, Integer inhabitedTimeFilter) throws IOException {
        System.out.println("Comparing worlds");
        Files.walkFileTree(expected, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dirA, BasicFileAttributes attrs) throws IOException {
                Path relative = expected.relativize(dirA);
                Path dirB = actual.resolve(relative);
                List<String> listA;
                List<String> listB;
                try (Stream<Path> streamA = Files.list(dirA);
                     Stream<Path> streamB = Files.list(dirB)) {
                    listA = streamA.map(path -> path.getFileName().toString()).toList();
                    listB = streamB.map(path -> path.getFileName().toString()).toList();
                }
                // Check that all expected files exist in B. It is fine if B has more
                // files/folders than expected. This for example happens since mch will
                // always restore a "region" files even though it might not have existed
                // in the original world.
                for (String fileA : listA) {
                    assertTrue(listB.contains(fileA), "Expected file " + fileA + " is missing.");
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path fileA, BasicFileAttributes attrs) throws IOException {
                Path relative = expected.relativize(fileA);
                Path fileB = actual.resolve(relative);
                if (fileA.toString().endsWith(".mca")) {
                    assertRegionFileEqual(fileA, fileB, inhabitedTimeFilter);
                } else {
                    byte[] bytesA = Files.readAllBytes(fileA);
                    byte[] bytesB = Files.readAllBytes(fileB);
                    assertArrayEquals(bytesA, bytesB, "File " + relative + " is not equal.");
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void assertRegionFileEqual(Path expected, Path actual, Integer inhabitedTimeFilter) throws IOException {
        assertTrue(Files.exists(actual), actual.getFileName() + " did not exist.");

        if (Files.size(expected) == 0 && Files.size(actual) == 0) {
            // Empty region files are okay, return now to avoid crashing when trying to
            // parse empty files.
            return;
        }
        Path fileName = expected.getFileName();
        try (McRegionFileReader readerA = new McRegionFileReader(expected);
             McRegionFileReader readerB = new McRegionFileReader(actual)
             ) {
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    boolean hasA = readerA.hasChunk(x, z);
                    boolean hasB = readerB.hasChunk(x, z);
                    NbtCompound nbtA = null;
                    NbtCompound nbtB = null;
                    if (inhabitedTimeFilter != null) {
                        if (hasA) {
                            try (DataInputStream streamA = readerA.readChunk(x, z)) {
                                nbtA = NbtTag.readCompound(streamA);
                            }
                            NbtTag inhabitedTime = nbtA.get("InhabitedTime");
                            if (inhabitedTime instanceof NbtLong nbtLong && nbtLong.getValue() <= inhabitedTimeFilter) {
                                // The chunk exists, but it should not be saved by mch since it is under the
                                // inhabited time filter. So pretend the chunk did not exist.
                                hasA = false;
                            }
                        }
                        if (hasB) {
                            try (DataInputStream streamB = readerB.readChunk(x, z)) {
                                nbtB = NbtTag.readCompound(streamB);
                            }
                            NbtTag inhabitedTime = nbtB.get("InhabitedTime");
                            if (inhabitedTime instanceof NbtLong nbtLong) {
                                assertFalse(
                                    nbtLong.getValue() <= inhabitedTimeFilter,
                                    "In region file " + fileName + ", the chunk at chunk coordinates " +
                                        "[" + x + "," + z + "] was saved even though it is under the InhabitedTime " +
                                        "filter. InhabitedTime is " + nbtLong.getValue()
                                );
                            }
                        }
                    }
                    assertFalse(
                        hasA && !hasB,
                        "In region file " + fileName + ", expected chunk at chunk coordinates " +
                            "[" + x + "," + z + "] to exist."
                    );
                    assertFalse(
                        !hasA && hasB,
                        "In region file " + fileName + ", the chunk at chunk coordinates " +
                            "[" + x + "," + z + "] has been hallucinated into creation ??"
                    );
                    if (!hasA) {
                        continue;
                    }
                    if (nbtA == null) {
                        try (DataInputStream streamA = readerA.readChunk(x, z)) {
                            nbtA = NbtTag.readCompound(streamA);
                        }
                    }
                    if (nbtB == null) {
                        try (DataInputStream streamB = readerB.readChunk(x, z)) {
                            nbtB = NbtTag.readCompound(streamB);
                        }
                    }
                    assertEquals(
                        nbtA,
                        nbtB,
                        "In region file " + fileName + ", the chunk at chunk coordinates " +
                            "[" + x + "," + z + "] did not have the expected nbt."
                    );
                }
            }
        }
    }
}
