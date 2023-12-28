package ca.bkaw.mch.operation;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.nbt.NbtLong;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.region.MchRegionFile;
import ca.bkaw.mch.region.RegionStorageVisitor;
import ca.bkaw.mch.region.mc.McRegionFileReader;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.world.RegionFileInfo;
import ca.bkaw.mch.world.WorldAccessor;
import ca.bkaw.mch.world.WorldProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for performing a commit.
 */
public class CommitOperation {
    public static void run(MchRepository repository, String commitMessage, boolean cache) throws IOException {
        long start = System.currentTimeMillis();

        // Read configuration and current commit from repository
        MchConfiguration configuration = repository.getConfiguration();
        Reference20<Commit> currentCommitReference = repository.getHeadCommit();
        Commit currentCommit = currentCommitReference != null && cache ? currentCommitReference.resolve(repository) : null;

        WorldContainer currentWorldContainer = currentCommit != null
            ? currentCommit.getWorldContainer().resolve(repository)
            : null;

        // The world container object that the commit will reference
        WorldContainer worldContainer = new WorldContainer();

        for (TrackedWorld trackedWorld : configuration.getTrackedWorlds()) {
            // Loop trough each world that this mch repository is tracking.
            WorldAccessor worldAccessor = trackedWorld.getWorldAccessor();
            try (WorldProvider worldProvider = worldAccessor.access(repository)) {

                // The world object for this version of the tracked world.
                World world = new World();

                World currentWorld = resolve(repository, currentWorldContainer != null
                    ? currentWorldContainer.getWorld(trackedWorld.getId())
                    : null);

                for (String dimensionKey : worldProvider.getDimensions()) {

                    System.out.println("Processing dimension " + dimensionKey);

                    Dimension currentDimension = resolve(repository, currentWorld != null
                        ? currentWorld.getDimension(dimensionKey)
                        : null);

                    Tree currentMiscellaneousFiles = resolve(repository, currentDimension != null ?
                        currentDimension.getMiscellaneousFiles()
                        : null);

                    // Track miscellaneous files
                    Reference20<Tree> treeReference = worldProvider.trackDirectoryTree(
                        dimensionKey,
                        repository,
                        str -> switch (str) {
                            case "region", "DIM1", "DIM-1", "dimensions", "mch" -> false;
                            default -> true;
                        },
                        currentMiscellaneousFiles
                    );

                    // The dimension object for this version of the dimension.
                    Dimension dimension = new Dimension(treeReference);

                    // Store region files

                    for (RegionFileInfo regionFileInfo : worldProvider.getRegionFiles(dimensionKey)) {
                        if (regionFileInfo.fileSize() == 0) {
                            // I am not sure how or why these empty region files are created, but since they
                            // are 0 bytes they do not contain valid data. Skip them to avoid crashing.
                            System.out.println("    Skipping empty file (0 bytes): " + regionFileInfo.fileName());
                            continue;
                        }

                        Dimension.RegionFileReference currentRegionFileInfo = currentDimension != null
                            ? currentDimension.getRegionFile(regionFileInfo.getRegionX(), regionFileInfo.getRegionZ())
                            : null;

                        if (currentRegionFileInfo != null && currentRegionFileInfo.getLastModifiedTime() == regionFileInfo.lastModified()) {
                            // The region file has not been modified since the last commit.
                            // We do not need to read this region file since we know that it has not been
                            // changed.
                            // We can simply reference the same version number that the previous commit has.
                            dimension.addRegionFile(currentRegionFileInfo);
                            System.out.println("    Not modified: " + regionFileInfo.fileName());
                            continue;
                        }

                        Path regionStoragePath = RegionStorageVisitor.getPath(
                            repository, trackedWorld, dimensionKey,
                            regionFileInfo.getRegionX(), regionFileInfo.getRegionZ()
                        );

                        Path mchRegionFilePath = MchRegionFile.getPath(
                            repository, trackedWorld, dimensionKey,
                            regionFileInfo.getRegionX(), regionFileInfo.getRegionZ()
                        );

                        Files.createDirectories(regionStoragePath.getParent());

                        // Store each chunk and record the version numbers of all the chunks.

                        System.out.println("    " + regionFileInfo.fileName());

                        int[] currentChunkVersionNumbers = currentRegionFileInfo != null
                            ? MchRegionFile.read(mchRegionFilePath, currentRegionFileInfo.getVersionNumber())
                            : null;

                        int[] chunkVersionNumbers = new int[1024];
                        try (
                            RandomAccessReader reader = worldProvider.openRegionFile(dimensionKey, regionFileInfo.fileName());
                            McRegionFileReader mcRegionFile = new McRegionFileReader(reader)
                        ) {
                            RegionStorageVisitor.visit(regionStoragePath, chunk -> {
                                if (mcRegionFile.hasChunk(chunk.getChunkX(), chunk.getChunkZ())) {
                                    // There is a chunk in the region file

                                    // Get when it was last modified
                                    int chunkLastModified = mcRegionFile.getChunkLastModified(chunk.getChunkX(), chunk.getChunkZ());

                                    if (currentChunkVersionNumbers != null) {
                                        // We can use the previous chunk version numbers to get the previous last
                                        // modified time.
                                        int currentChunkVersionNumber = currentChunkVersionNumbers[chunk.getIndex()];

                                        // If there was no chunk in the last commit the version number is zero.
                                        // Otherwise, check if it has been changed since last commit.
                                        if (currentChunkVersionNumber != 0 && chunk.getLastModified(currentChunkVersionNumber) == chunkLastModified) {
                                            // The chunk has not been modified since the last commit.
                                            // We do not need to store it again.
                                            chunkVersionNumbers[chunk.getIndex()] = currentChunkVersionNumber;
                                            return;
                                        }
                                    }

                                    // Read the chunk nbt
                                    NbtCompound chunkNbt = mcRegionFile.readChunkNbt(chunk.getChunkX(), chunk.getChunkZ());

                                    NbtTag inhabitedTime = chunkNbt.get("InhabitedTime");

                                    // TODO make this configurable per repo.
                                    //  Default should probably be to be 100% lossless
                                    if (inhabitedTime instanceof NbtLong nbtLong && nbtLong.getValue() <= 0) {
                                        // Skipping chunk because it has not been inhabited by players.
                                        chunkVersionNumbers[chunk.getIndex()] = 0;
                                    } else {
                                        // Store the chunk
                                        int chunkVersionNumber = chunk.store(chunkNbt, chunkLastModified);

                                        // Save the version number of the chunk
                                        chunkVersionNumbers[chunk.getIndex()] = chunkVersionNumber;
                                    }
                                } else {
                                    // There is no chunk. The version number is 0.
                                    chunkVersionNumbers[chunk.getIndex()] = 0;
                                }
                            });
                        }

                        // Use the chunk version numbers to create a region file version number

                        int regionFileVersionNumber = MchRegionFile.store(mchRegionFilePath, chunkVersionNumbers);

                        // Add the region file to the dimension object
                        Dimension.RegionFileReference regionFileReference = new Dimension.RegionFileReference(
                            regionFileInfo.getRegionX(),
                            regionFileInfo.getRegionZ(),
                            regionFileVersionNumber,
                            regionFileInfo.lastModified()
                        );
                        dimension.addRegionFile(regionFileReference);
                    }

                    // Save the dimension object and add it to the world
                    world.addDimension(
                        dimensionKey,
                        ObjectStorageTypes.DIMENSION.save(dimension, repository)
                    );
                }

                // Save the world object and add it to the world container
                worldContainer.addWorld(
                    trackedWorld.getId(),
                    ObjectStorageTypes.WORLD.save(world, repository)
                );
            }
        }

        // Save the world container object
        Reference20<WorldContainer> worldContainerReference
            = ObjectStorageTypes.WORLD_CONTAINER.save(worldContainer, repository);

        // Create the commit object
        Commit commit = new Commit(
            commitMessage,
            System.currentTimeMillis(),
            worldContainerReference,
            currentCommitReference
        );

        // Save the commit object
        Reference20<Commit> commitReference = ObjectStorageTypes.COMMIT.save(commit, repository);

        repository.setHeadCommit(commitReference);

        long end = System.currentTimeMillis();
        long ms = end - start;
        System.out.println("Done. Took: " + ms + " ms");

        System.out.println("commit hash: " + commitReference.getSha1().asHex());
    }

    private static <T extends StorageObject> T resolve(MchRepository repository, @Nullable Reference20<T> reference) throws IOException {
        if (reference == null) {
            return null;
        }
        return reference.resolve(repository);
    }
}
