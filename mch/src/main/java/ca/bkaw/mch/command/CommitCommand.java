package ca.bkaw.mch.command;

import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.tree.TreeTracker;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.region.MchRefRegionFile;
import ca.bkaw.mch.region.MchRegionFileVisitor;
import ca.bkaw.mch.region.mc.McRegionFile;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.world.RegionFileInfo;
import ca.bkaw.mch.world.WorldProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommitCommand {
    public static void run(MchRepository repository, String commitMessage) throws IOException {
        // Read configuration and current commit from repository
        MchConfiguration configuration = repository.getConfiguration();
        Reference20<Commit> currentCommitReference = repository.getHeadCommit();
        Commit currentCommit = currentCommitReference != null ? currentCommitReference.resolve(repository) : null;

        WorldContainer currentWorldContainer = currentCommit != null
            ? currentCommit.getWorldContainer().resolve(repository)
            : null;

        // The world container object that the commit will reference
        WorldContainer worldContainer = new WorldContainer();

        for (TrackedWorld trackedWorld : configuration.getTrackedWorlds()) {
            // Loop trough each world that this mch repository is tracking.
            WorldProvider worldProvider = trackedWorld.getWorldProvider();

            // The world object for this version of the tracked world.
            World world = new World();

            World currentWorld = resolve(repository, currentWorldContainer != null
                ? currentWorldContainer.getWorld(trackedWorld.getId())
                : null);

            for (String dimensionKey : worldProvider.getDimensions()) {

                // The dimension object for this version of the dimension.
                Dimension dimension = new Dimension();

                Dimension currentDimension = resolve(repository, currentWorld != null
                    ? currentWorld.getDimension(dimensionKey)
                    : null);

                // Track files
                // TODO tracking files with a WorldProvider
                Reference20<Tree> treeReference = TreeTracker.trackDirectoryTree(
                    repository,
                    /* directoryPath */ null,
                    str -> switch (str) {
                        case "region", "DIM1", "DIM-1", "dimensions", "mch" -> false;
                        default -> true;
                    }
                );

                // Store region files

                System.out.println("Processing dimension " + dimensionKey);

                for (RegionFileInfo regionFileInfo : worldProvider.getRegionFiles(dimensionKey)) {
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

                    Path mchRegionFolderPath = repository.getRoot()
                        .resolve("world")
                        .resolve(trackedWorld.getId().asHex())
                        .resolve("dimensions")
                        .resolve(dimensionKey.replace(':', '_'))
                        .resolve("region");

                    Path mchRegionFilePath = mchRegionFolderPath
                        .resolve(regionFileInfo.fileName().replace(".mca", ".mchrc"));

                    Path mchRefRegionFilePath = mchRegionFolderPath
                        .resolve(regionFileInfo.fileName().replace(".mca", ".mchrv"));

                    Files.createDirectories(mchRegionFilePath.getParent());

                    // Store each chunk and record the version numbers of all the chunks.

                    System.out.println("    " + regionFileInfo.fileName());

                    int[] currentChunkVersionNumbers = currentRegionFileInfo != null
                        ? MchRefRegionFile.read(mchRefRegionFilePath, currentRegionFileInfo.getVersionNumber())
                        : null;

                    int[] chunkVersionNumbers = new int[1024];
                    try (
                        RandomAccessReader reader = worldProvider.openRegionFile(dimensionKey, regionFileInfo.fileName());
                        McRegionFile mcRegionFile = new McRegionFile(reader)
                    ) {
                        MchRegionFileVisitor.visit(mchRegionFilePath, chunk -> {
                            if (mcRegionFile.hasChunk(chunk.getChunkX(), chunk.getChunkZ())) {
                                int chunkLastModified = mcRegionFile.getChunkLastModified(chunk.getChunkX(), chunk.getChunkZ());
                                if (currentChunkVersionNumbers != null
                                    && chunk.getLastModified() == chunkLastModified) {
                                    // The chunk has not been modified since the last commit.
                                    // We do not need to store it again.
                                    chunkVersionNumbers[chunk.getIndex()] = currentChunkVersionNumbers[chunk.getIndex()];
                                    return;
                                }
                                // Read the chunk nbt
                                NbtCompound chunkNbt = mcRegionFile.readChunkNbt(chunk.getChunkX(), chunk.getChunkZ());

                                // Store the chunk
                                int chunkVersionNumber = chunk.store(chunkNbt);

                                chunk.setLastModified(chunkLastModified);

                                // Save the version number of the chunk
                                chunkVersionNumbers[chunk.getIndex()] = chunkVersionNumber;
                            } else {
                                // There is no chunk. The version number is 0.
                                chunkVersionNumbers[chunk.getIndex()] = 0;
                            }
                        });
                    }

                    // Use the chunk version numbers to create a region file version number

                    int regionFileVersionNumber = MchRefRegionFile.createUghImTired(mchRefRegionFilePath, chunkVersionNumbers);

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

        System.out.println("commit hash: " + commitReference.getSha1().asHex());
    }

    private static <T extends StorageObject> T resolve(MchRepository repository, @Nullable Reference20<T> reference) throws IOException {
        if (reference == null) {
            return null;
        }
        return reference.resolve(repository);
    }
}
