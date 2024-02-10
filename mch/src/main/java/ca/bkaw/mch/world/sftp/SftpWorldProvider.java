package ca.bkaw.mch.world.sftp;

import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.tree.Tree;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.util.RandomAccessReader;
import ca.bkaw.mch.util.Util;
import ca.bkaw.mch.world.RegionFileInfo;
import ca.bkaw.mch.world.WorldProvider;
import ca.bkaw.mch.world.ftp.RandomAccessTempFileImpl;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SftpWorldProvider implements WorldProvider {
    private final SSHClient sshClient;
    private final SFTPClient sftp;
    private final String worldPath;

    public SftpWorldProvider(SftpProfile sftpProfile, MchRepository repository, String worldPath) throws IOException {
        this.worldPath = worldPath;
        this.sshClient = sftpProfile.connect(repository);
        this.sftp = this.sshClient.newSFTPClient();
    }

    @Override
    public List<String> getDimensions() throws IOException {
        List<RemoteResourceInfo> directories = this.sftp.ls(this.worldPath);

        List<String> dimensions = new ArrayList<>(3);
        for (RemoteResourceInfo directory : directories) {
            switch (directory.getName()) {
                case "region" -> dimensions.add(Dimension.OVERWORLD);
                case Util.NETHER_FOLDER -> dimensions.add(Dimension.NETHER);
                case Util.THE_END_FOLDER -> dimensions.add(Dimension.THE_END);
            }
        }
        // TODO custom dimensions
        return dimensions;
    }

    private String getDimensionPath(String dimension) {
        return switch (dimension) {
            case Dimension.OVERWORLD -> this.worldPath;
            case Dimension.NETHER -> this.worldPath + '/' + Util.NETHER_FOLDER;
            case Dimension.THE_END -> this.worldPath + '/' + Util.THE_END_FOLDER;
            default -> this.worldPath + "/dimensions/" + dimension.replace(':', '/');
        };
    }

    @Override
    public List<RegionFileInfo> getRegionFiles(String dimension) throws IOException {
        String path = this.getDimensionPath(dimension) + "/region";

        return this.sftp.ls(path,
                file -> file.getName().startsWith("r.") && file.getName().endsWith(".mca")
            )
            .stream()
            .map(file -> new RegionFileInfo(
                file.getName(),
                file.getAttributes().getMtime(),
                file.getAttributes().getSize()
            ))
            .toList();
    }

    @Override
    public RandomAccessReader openRegionFile(String dimension, String regionFileName, long estimatedSize) throws IOException {
        String path = this.getDimensionPath(dimension) + "/region/" + regionFileName;
        Path tempFilePath = Files.createTempFile("sftp_" + regionFileName, ".mca");
        File tempFile = tempFilePath.toFile();
        this.sftp.get(path, new FileSystemFile(tempFile));
        // RandomAccessTempFileImpl will delete the file when the reader is closed.
        return new RandomAccessTempFileImpl(tempFile);
    }

    @Override
    public Reference20<Tree> trackDirectoryTree(String dimension, MchRepository repository, Predicate<String> predicate, @Nullable Tree currentTree) throws IOException {
        return this.trackDirectoryTreePath(this.getDimensionPath(dimension), repository, predicate, currentTree);
    }

    public Reference20<Tree> trackDirectoryTreePath(String path, MchRepository repository, Predicate<String> predicate, @Nullable Tree currentTree) throws IOException {
        Tree tree = new Tree();
        for (RemoteResourceInfo file : this.sftp.ls(path)) {
            String name = file.getName();
            if (!predicate.test(name)) {
                continue;
            }
            // TODO repository-wide "mchignore"
            if (name.contains("ledger.sqlite")) {
                continue;
            }
            if (file.isDirectory()) {
                // Track subdirectories
                Reference20<Tree> currentSubTreeReference = currentTree != null ? currentTree.getSubTrees().get(name) : null;
                Tree currentSubTree = currentSubTreeReference != null ? currentSubTreeReference.resolve(repository) : null;
                Reference20<Tree> subDirectoryReference = trackDirectoryTreePath(file.getPath(), repository, str -> true, currentSubTree);
                tree.addSubTree(name, subDirectoryReference);
            } else if (file.isRegularFile()) {
                // Track files
                Tree.BlobReference currentBlobReference = currentTree != null ? currentTree.getFiles().get(name) : null;
                long lastModified = file.getAttributes().getMtime();
                if (currentBlobReference == null || currentBlobReference.lastModified() != lastModified) {
                    // The file has changed since last commit. Save it anew.
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    this.sftp.get(file.getPath(), new OutputStreamFileDest(stream));
                    Blob blob = new Blob(stream.toByteArray());
                    Reference20<Blob> blobReference = ObjectStorageTypes.BLOB.save(blob, repository);
                    tree.addFile(name, new Tree.BlobReference(blobReference, lastModified));
                } else {
                    // The file has not changed since last commit. Reuse the reference.
                    tree.addFile(name, currentBlobReference);
                }
            }
        }

        // Save the tree
        return ObjectStorageTypes.TREE.save(tree, repository);
    }

    @Override
    public void close() throws IOException {
        this.sshClient.close();
        this.sftp.close();
    }
}
