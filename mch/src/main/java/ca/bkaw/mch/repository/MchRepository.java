package ca.bkaw.mch.repository;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.chunk.RegionFileChunk;
import ca.bkaw.mch.object.ObjectStorageType;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.object.world.World;
import ca.bkaw.mch.object.worldcontainer.WorldContainer;
import ca.bkaw.mch.region.MchRegionFile;
import ca.bkaw.mch.region.RegionStorageVisitor;
import ca.bkaw.mch.region.mc.McRegionFileWriter;
import ca.bkaw.mch.util.StringPath;
import ca.bkaw.mch.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class MchRepository implements RepositoryAccess {
    /**
     * The root of the repository.
     */
    private final Path root;
    private MchConfiguration configuration;

    public MchRepository(Path root) {
        this.root = root;
    }

    /**
     * Create the directories needed for this repository.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void createDirectories() throws IOException {
        for (ObjectStorageType<?> objectStorageType : ObjectStorageTypes.values()) {
            objectStorageType.createDirectories(this);
        }
    }

    private Path getConfigurationPath() {
        return this.root.resolve("configuration");
    }

    public void readConfiguration() throws IOException {
        Path path = this.getConfigurationPath();
        if (Files.notExists(path)) {
            this.configuration = new MchConfiguration();
            return;
        }
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            this.configuration = new MchConfiguration(stream);
        }
    }

    public void saveConfiguration() throws IOException {
        Path path = this.getConfigurationPath();
        try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            this.configuration.write(stream);
        }
    }

    private Path getHeadPath() {
        return this.root.resolve("head");
    }

    @Override
    @Nullable
    public Reference20<Commit> getHeadCommit() throws IOException {
        Path path = this.getHeadPath();
        if (Files.notExists(path)) {
            return null;
        }
        String str = Files.readString(path).trim();
        if (str.isEmpty()) {
            return null;
        }
        Sha1 sha1 = Sha1.fromString(str);
        return new Reference20<>(ObjectStorageTypes.COMMIT, sha1);
    }

    /**
     * Set the head commit, the latest commit.
     *
     * @param commitReference The reference to the commit, or null.
     * @throws IOException If an I/O error occurs.
     */
    public void setHeadCommit(@Nullable Reference20<Commit> commitReference) throws IOException {
        Path path = this.getHeadPath();
        String str = commitReference == null ? "" : commitReference.getSha1().asHex();
        Files.writeString(path, str);
    }

    /**
     * Check if this repository has been created.
     *
     * @return Whether the repository exists.
     */
    public boolean exists() {
        return Files.exists(this.getConfigurationPath());
    }

    /**
     * Get the path to the root of the repository. Also known as the "mch" directory.
     *
     * @return The path.
     */
    public Path getRoot() {
        return this.root;
    }

    /**
     * Get the directory where the user initialized the repository.
     * <p>
     * This is the directory that contains the {@code mch} directory as gotten by
     * {@link #getRoot()}.
     *
     * @return The project directory path.
     */
    public Path getProjectDirectory() {
        return this.root.getParent();
    }

    /**
     * Get the {@link MchConfiguration configuration}.
     *
     * @return The configuration.
     */
    public MchConfiguration getConfiguration() {
        if (this.configuration == null) {
            throw new IllegalStateException("Configuration has not been read.");
        }
        return this.configuration;
    }

    @Override
    public List<Sha1> getTrackedWorlds() {
        return this.getConfiguration().getTrackedWorlds().stream().map(TrackedWorld::getId).toList();
    }

    @Override
    public @Nullable Sha1 getTrackedWorld(String name) {
        TrackedWorld trackedWorld = this.getConfiguration().getTrackedWorld(name);
        if (trackedWorld == null) {
            return null;
        }
        return trackedWorld.getId();
    }

    @Override
    public List<String> getDimensions(Sha1 commitSha1, Sha1 worldSha1) throws IOException {
        Commit commit = ObjectStorageTypes.COMMIT.read(commitSha1, this);
        WorldContainer worldContainer = commit.getWorldContainer().resolve(this);
        Reference20<World> worldRef = worldContainer.getWorld(worldSha1);
        if (worldRef == null) {
            return List.of();
        }
        World world = worldRef.resolve(this);
        return world.getDimensions().keySet().stream().toList();
    }

    @Override
    public Commit accessCommit(Sha1 commitSha1) throws IOException {
        return ObjectStorageTypes.COMMIT.read(commitSha1, this);
    }

    @Override
    public @Nullable DimensionAccess accessDimension(Sha1 commitSha1, Sha1 worldSha1, String dimensionKey) throws IOException {
        MchRepository repository = this;
        TrackedWorld trackedWorld = repository.getConfiguration().getTrackedWorld(worldSha1);
        Commit commit = ObjectStorageTypes.COMMIT.read(commitSha1, repository);
        WorldContainer worldContainer = commit.getWorldContainer().resolve(repository);
        Reference20<World> worldRef = worldContainer.getWorld(worldSha1);
        if (worldRef == null) {
            return null;
        }
        World world = worldRef.resolve(repository);
        Reference20<Dimension> dimensionRef = world.getDimension(dimensionKey);
        if (dimensionRef == null) {
            return null;
        }
        Dimension dimension = dimensionRef.resolve(repository);

        return new DimensionAccess() {
            @Override
            @Nullable
            public InputStream restoreFile(StringPath path) throws IOException {
                if (path.toString().startsWith("region/")) {
                    String fileName = path.getFileName();
                    if (!fileName.startsWith("r.") || !fileName.endsWith(".mca")) {
                        return null;
                    }
                    String str = fileName.substring("r.".length(), fileName.length() - ".mca".length());
                    String[] split = str.split("\\.");
                    int regionX = Integer.parseInt(split[0]);
                    int regionZ = Integer.parseInt(split[1]);
                    Dimension.RegionFileReference regionFileRef = dimension.getRegionFile(regionX, regionZ);

                    if (regionFileRef == null) {
                        return null;
                    }

                    Path regionStoragePath = RegionStorageVisitor.getPath(
                        repository, trackedWorld, dimensionKey, regionX, regionZ
                    );

                    Path mchRegionFilePath = MchRegionFile.getPath(
                        repository, trackedWorld, dimensionKey, regionX, regionZ
                    );

                    Path mcRegionFilePath = Files.createTempFile("restore_" + fileName, ".mca");
                    System.out.println("Writing to temp mca file " + mcRegionFilePath);
                    try (McRegionFileWriter regionFile = new McRegionFileWriter(mcRegionFilePath)) {
                        if (true && Files.exists(mchRegionFilePath)) { // temp allow reading corrupted repos
                        int[] chunkVersionNumbers = MchRegionFile.read(mchRegionFilePath, regionFileRef.getVersionNumber());

                        RegionStorageVisitor.visitReadOnly(regionStoragePath, chunk -> {
                            int chunkVersionNumber = chunkVersionNumbers[chunk.getIndex()];
                            if (chunkVersionNumber != 0) {
                                RegionFileChunk restoredChunk = chunk.restore(chunkVersionNumber);
                                regionFile.writeChunk(restoredChunk.nbt(), restoredChunk.lastModified());
                            }
                        });
                        } // temp allow reading corrupted repos
                    }
                    return Files.newInputStream(mcRegionFilePath);
                }

                // Non-region file.
                Tree tree = dimension.getMiscellaneousFiles().resolve(repository);
                for (int i = 0; i < path.getNameCount() - 1; i++) {
                    String name = path.getName(i);
                    Reference20<Tree> subTreeRef = tree.getSubTrees().get(name);
                    if (subTreeRef == null) {
                        return null;
                    }
                    tree = subTreeRef.resolve(repository);
                }
                String fileName = path.getFileName();
				if (!tree.getFiles().containsKey(fileName)) {
					return null;
				}
				Tree.BlobReference blobRef = tree.getFiles().get(fileName);
				Blob blob = blobRef.reference().resolve(repository);
				return new ByteArrayInputStream(blob.getBytes());
			}

            @Override
            @NotNull
            public List<String> list(StringPath path) throws IOException {
                if (path.toString().startsWith("region/")) {
                    if (path.getNameCount() != 1) {
                        // No files in subdirectories of region.
                        return List.of();
                    }
                    return dimension.getRegionFiles().stream().map(
                        (region) -> Util.formatRegionFileName(region.getRegionX(), region.getRegionZ(), ".mca")
                    ).toList();
                }

                // Non-region file.
                Tree tree = dimension.getMiscellaneousFiles().resolve(repository);
                for (int i = 0; i < path.getNameCount(); i++) {
                    String name = path.getName(i);
                    if (name.isEmpty() || ".".equals(name)) {
                        continue;
                    }
                    Reference20<Tree> subTreeRef = tree.getSubTrees().get(name);
                    if (subTreeRef == null) {
                        return List.of();
                    }
                    tree = subTreeRef.resolve(repository);
                }

                List<String> list = new ArrayList<>(tree.getFiles().size() + tree.getSubTrees().size());
				list.addAll(tree.getFiles().keySet());
				list.addAll(tree.getSubTrees().keySet());
                return list;
            }
        };
    }
}
