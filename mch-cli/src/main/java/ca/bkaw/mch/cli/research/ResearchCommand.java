package ca.bkaw.mch.cli.research;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.chunk.RegionFileChunk;
import ca.bkaw.mch.cli.GuiceFactory.WorkingDir;
import ca.bkaw.mch.nbt.NbtLong;
import ca.bkaw.mch.nbt.NbtTag;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.region.MchRegionFile;
import ca.bkaw.mch.region.RegionStorageVisitor;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Command(name = "research")
public class ResearchCommand {
    @Inject
    MchRepository repository;

    @Inject
    @WorkingDir
    private Path workingDir;

    @Command(name = "inhabitedtime-csv")
    public void inhabitedTimeCsv(
        @Parameters(index = "0")
        String commitHash
    ) throws IOException {
        // Quick and dirty (and ugly) code that gets the job done :p
        Path outputFilePath = workingDir.resolve("mch-research/InhabitedTime.csv");
        Files.createDirectories(outputFilePath.getParent());
        PrintWriter output = new PrintWriter(Files.newBufferedWriter(outputFilePath));

        MchConfiguration configuration = repository.getConfiguration();
        Commit commit = ObjectStorageTypes.COMMIT.read(Sha1.fromString(commitHash), repository);
        WorldContainer worldContainer = commit.getWorldContainer().resolve(repository);

        for (Map.Entry<Sha1, Reference20<World>> entry : worldContainer.getWorlds().entrySet()) {
            Sha1 worldId = entry.getKey();
            TrackedWorld trackedWorld = configuration.getTrackedWorld(worldId);
            World world = entry.getValue().resolve(repository);

            for (Map.Entry<String, Reference20<Dimension>> entry2 : world.getDimensions().entrySet()) {
                String dimensionKey = entry2.getKey();
                System.out.println("Processing dimension " + dimensionKey);

                Dimension dimension = entry2.getValue().resolve(this.repository);

                for (Dimension.RegionFileReference regionFileRef : dimension.getRegionFiles()) {
                    int[] chunkVersionNumbers = MchRegionFile.read(
                        this.repository, trackedWorld, dimensionKey,
                        regionFileRef.getRegionX(), regionFileRef.getRegionZ(),
                        regionFileRef.getVersionNumber()
                    );
                    RegionStorageVisitor.visitReadOnly(
                        this.repository, trackedWorld, dimensionKey,
                        regionFileRef.getRegionX(), regionFileRef.getRegionZ(),
                        chunk -> {
                            int chunkVersionNumber = chunkVersionNumbers[chunk.getIndex()];
                            if (chunkVersionNumber != 0) {
                                RegionFileChunk restoredChunk = chunk.restore(chunkVersionNumber);
                                NbtTag nbtTag = restoredChunk.nbt().get("InhabitedTime");
                                if (nbtTag == null) {
                                    output.println("null");
                                } else if (nbtTag instanceof NbtLong nbtLong) {
                                    output.println(nbtLong.getValue());
                                } else {
                                    throw new RuntimeException("Weird InhabitedTime type. " + nbtTag);
                                }
                            }
                        });
                }
            }
        }

        System.out.println("Created " + outputFilePath);
        output.close();
    }

}
