package ca.bkaw.mch.command;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.region.MchRegionFileVisitor;
import ca.bkaw.mch.region.mc.McRegionFile;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.world.RegionFileInfo;
import ca.bkaw.mch.world.WorldProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommitCommand {
    public static void run(MchRepository repository, String commitMessage) throws IOException {
        MchConfiguration configuration = repository.getConfiguration();

        WorldContainer worldContainer = new WorldContainer();

        for (TrackedWorld trackedWorld : configuration.getTrackedWorlds()) {
            WorldProvider worldProvider = trackedWorld.getWorldProvider();
            World world = new World();
            for (String dimensionKey : worldProvider.getDimensions()) {
                Dimension dimension = new Dimension();
                for (RegionFileInfo regionFileInfo : worldProvider.getRegionFiles(dimensionKey)) {
                    Path mchRegionFilePath = repository.getRoot()
                        .resolve("world")
                        .resolve(trackedWorld.getId().asHex())
                        .resolve("dimensions")
                        .resolve(dimensionKey.replace(':', '_'))
                        .resolve("region")
                        .resolve(regionFileInfo.fileName().replace(".mca", ".mchr"));
                    Files.createDirectories(mchRegionFilePath.getParent());
                    try (RandomAccessReader reader = worldProvider.openRegionFile(dimensionKey, regionFileInfo.fileName())) {
                        McRegionFile mcRegionFile = new McRegionFile(reader);
                        MchRegionFileVisitor.visit(mchRegionFilePath, chunk -> {
                            if (mcRegionFile.hasChunk(chunk.getChunkX(), chunk.getChunkZ())) {
                                NbtCompound chunkNbt = mcRegionFile.readChunkNbt(chunk.getChunkX(), chunk.getChunkZ());
                                chunk.store(chunkNbt);
                            } else {
                                // TODO what if the chunk wasn't empty but now is (was emptied by external program)
                            }
                        });
                        Dimension.RegionFileReference regionFileReference = new Dimension.RegionFileReference(
                            regionFileInfo.getRegionX(),
                            regionFileInfo.getRegionZ(),
                            0,
                            // TODO did we forget about region version numbers?
                            regionFileInfo.lastModified()
                        );
                        dimension.addRegionFile(regionFileReference);
                    }
                }
                Sha1 dimensionSha1 = ObjectStorageTypes.DIMENSION.save(dimension, repository);
                Reference20<Dimension> dimensionReference = new Reference20<>(ObjectStorageTypes.DIMENSION, dimensionSha1);
                world.addDimension(dimensionKey, dimensionReference);
            }
            Sha1 worldSha1 = ObjectStorageTypes.WORLD.save(world, repository);
            Reference20<World> worldReference = new Reference20<>(ObjectStorageTypes.WORLD, worldSha1);
            worldContainer.addWorld(trackedWorld.getId(), worldReference);
        }

        Sha1 worldContainerSha1
            = ObjectStorageTypes.WORLD_CONTAINER.save(worldContainer, repository);
        Reference20<WorldContainer> worldContainerReference
            = new Reference20<>(ObjectStorageTypes.WORLD_CONTAINER, worldContainerSha1);

        Commit commit = new Commit(commitMessage, System.currentTimeMillis(), worldContainerReference);

        Sha1 commitSha1 = ObjectStorageTypes.COMMIT.save(commit, repository);

        System.out.println("commit hash: " + commitSha1.asHex());
    }
}
