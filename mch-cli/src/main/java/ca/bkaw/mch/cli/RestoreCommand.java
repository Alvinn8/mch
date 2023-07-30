package ca.bkaw.mch.cli;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.nbt.NbtCompound;
import ca.bkaw.mch.object.ObjectNotFoundException;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.region.MchRefRegionFile;
import ca.bkaw.mch.region.MchRegionFileVisitor;
import ca.bkaw.mch.region.mc.McRegionFileWriter;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.util.Util;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "restore")
public class RestoreCommand implements Callable<Integer> {
    @Inject
    MchRepository repository;

    @Parameters(index = "0")
    String commitHash;

    @Override
    public Integer call() throws IOException {
        // Get the commit to restore to
        Commit commit;
        try {
            commit = ObjectStorageTypes.COMMIT.read(Sha1.fromString(this.commitHash), this.repository);
        } catch (ObjectNotFoundException e) {
            System.err.println(e.getMessage());
            return ExitCode.USAGE;
        }

        // Find an available path we can restore to.
        Path path = findAvaliablePath(this.repository.getProjectDirectory(), "mch-restore");
        Files.createDirectory(path);

        MchConfiguration configuration = this.repository.getConfiguration();

        WorldContainer worldContainer = commit.getWorldContainer().resolve(this.repository);
        for (Map.Entry<Sha1, Reference20<World>> entry : worldContainer.getWorlds().entrySet()) {
            Sha1 worldId = entry.getKey();
            TrackedWorld trackedWorld = configuration.getTrackedWorld(worldId);

            System.out.println("Restoring world " + trackedWorld.getName());

            // The snapshot of the world we want to restore to
            World world = entry.getValue().resolve(this.repository);

            Path worldPath = path.resolve(trackedWorld.getName());
            Files.createDirectories(worldPath);

            for (Map.Entry<String, Reference20<Dimension>> entry2 : world.getDimensions().entrySet()) {
                String dimensionKey = entry2.getKey();
                System.out.println("Restoring dimension " + dimensionKey);

                // The snapshot of the dimension we want to restore to
                Dimension dimension = entry2.getValue().resolve(this.repository);

                Path dimensionPath = Util.getDimensionPath(worldPath, dimensionKey);
                Files.createDirectories(dimensionPath);

                // Restore miscellaneous files
                Tree miscellaneousFiles = dimension.getMiscellaneousFiles().resolve(this.repository);
                this.restoreTree(miscellaneousFiles, dimensionPath);

                // Restore region files
                Path regionFolderPath = dimensionPath.resolve("region");
                Files.createDirectories(regionFolderPath);
                for (Dimension.RegionFileReference regionFileReference : dimension.getRegionFiles()) {

                    Path mchRegionFolderPath = Util.getMchRegionFolderPath(this.repository, trackedWorld, dimensionKey);

                    String regionFileName = "r." + regionFileReference.getRegionX() + "." + regionFileReference.getRegionZ() + ".mca";
                    System.out.println("    " + regionFileName);

                    // TODO this is duplicate code from commit (kinda)
                    Path mchRegionFilePath = mchRegionFolderPath
                        .resolve(regionFileName.replace(".mca", ".mchrc"));

                    Path mchRefRegionFilePath = mchRegionFolderPath
                        .resolve(regionFileName.replace(".mca", ".mchrv"));

                    Path mcRegionFilePath = regionFolderPath.resolve(regionFileName);
                    try (McRegionFileWriter regionFile = new McRegionFileWriter(mcRegionFilePath)) {
                        int[] chunkVersionNumbers = MchRefRegionFile.read(mchRefRegionFilePath, regionFileReference.getVersionNumber());
                        MchRegionFileVisitor.visitReadOnly(mchRegionFilePath, chunk -> {
                            int chunkVersionNumber = chunkVersionNumbers[chunk.getIndex()];
                            if (chunkVersionNumber != 0) {
                                NbtCompound chunkNbt = chunk.restore(chunkVersionNumber);
                                regionFile.writeChunk(chunkNbt, chunk.getLastModified());
                            }
                        });
                    }
                }
            }
        }

        System.out.println("Restored commit " + commitHash + " to " + Path.of(".").relativize(path));

        return ExitCode.OK;
    }

    private void restoreTree(Tree tree, Path path) throws IOException {
        // Restore files
        for (Map.Entry<String, Reference20<Blob>> entry : tree.getFiles().entrySet()) {
            String fileName = entry.getKey();
            Path filePath = path.resolve(fileName);

            Blob blob = entry.getValue().resolve(this.repository);
            Files.write(filePath, blob.getBytes());
        }

        // Restore subtrees
        for (Map.Entry<String, Reference20<Tree>> entry : tree.getSubTrees().entrySet()) {
            String directoryName = entry.getKey();
            Path directoryPath = path.resolve(directoryName);
            Files.createDirectories(directoryPath);

            Tree subTree = entry.getValue().resolve(this.repository);
            this.restoreTree(subTree, directoryPath);
        }
    }

    private Path findAvaliablePath(Path parent, String name) {
        Path path = parent.resolve(name);
        int i = 1;
        String newName = null;
        while (Files.exists(path)) {
            newName = name + "-" + i;
            path = parent.resolve(newName);
            i++;
        }
        if (i > 1) {
            System.out.println(name + " was occupied, using " + newName + " instead.");
        }
        return path;
    }
}
