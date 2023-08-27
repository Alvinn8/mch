package ca.bkaw.mch.world.ftp;

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
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * An active connection to an external FTP server that provides information about
 * a world by reading from an external FTP server.
 * <p>
 * Close the object to disconnect from the FTP server.
 */
public class FtpWorldProvider implements WorldProvider, AutoCloseable {
    private final FTPClient ftp;
    private final String worldPath;

    /**
     * Create a new provider and connect to the FTP server.
     *
     * @param profile Information on how to connect to the FTP server.
     * @param worldPath The path where the world can be found on the remote server.
     * @throws IOException If an I/O error occurs while connecting.
     */
    public FtpWorldProvider(FtpProfile profile, String worldPath) throws IOException {
        this.ftp = profile.connect();

        this.ftp.changeWorkingDirectory(worldPath);
        this.worldPath = this.ftp.printWorkingDirectory();
    }

    @Override
    public List<String> getDimensions() throws IOException {
        FTPFile[] directories = this.ftp.listDirectories();
        if (!FTPReply.isPositiveCompletion(this.ftp.getReplyCode())) {
            System.out.println("this.ftp.listFiles() = " + Arrays.toString(this.ftp.listFiles()));
            System.out.println("directories = " + Arrays.toString(directories));
            System.out.println("this.ftp.printWorkingDirectory() = " + this.ftp.printWorkingDirectory());
            throw new RuntimeException("Failed to list from FTP server. " + this.ftp.getReplyString());
        }
        this.ftp.retrieveFile("hello.txt", System.out);
        List<String> dimensions = new ArrayList<>(3);
        for (FTPFile directory : directories) {
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
        return Arrays.stream(this.ftp.listFiles(path,
                file -> file.getName().startsWith("r.") && file.getName().endsWith(".mca"))
            )
            .map(file -> new RegionFileInfo(
                file.getName(),
                file.getTimestampInstant().toEpochMilli())
            )
            .toList();
    }

    @Override
    public RandomAccessReader openRegionFile(String dimension, String regionFileName) throws IOException {
        String path = this.getDimensionPath(dimension) + "/region/" + regionFileName;
        Path tempFilePath = Files.createTempFile("ftp_" + regionFileName, ".mca");
        try (OutputStream stream = Files.newOutputStream(tempFilePath)) {
            this.ftp.retrieveFile(path, stream);
        }
        // RandomAccessTempFileImpl will delete the file when the reader is closed.
        return new RandomAccessTempFileImpl(tempFilePath.toFile());
    }

    @Override
    public Reference20<Tree> trackDirectoryTree(String dimension, MchRepository repository, Predicate<String> predicate) throws IOException {
        String dimensionPath = this.getDimensionPath(dimension);
        this.ftp.changeWorkingDirectory(dimensionPath);
        return this.trackDirectoryTree(repository, predicate);
    }

    private Reference20<Tree> trackDirectoryTree(MchRepository repository, Predicate<String> predicate) throws IOException {
        Tree tree = new Tree();
        for (FTPFile file : this.ftp.listFiles()) {
            String name = file.getName();
            if (!predicate.test(name)) {
                continue;
            }
            if (file.isDirectory()) {
                // Track subdirectories
                this.ftp.changeWorkingDirectory(name);
                Reference20<Tree> subDirectoryReference = trackDirectoryTree(repository, str -> true);
                tree.addSubTree(name, subDirectoryReference);
                this.ftp.changeToParentDirectory();
            } else if (file.isFile()) {
                // Track files
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                this.ftp.retrieveFile(name, stream);
                Blob blob = new Blob(stream.toByteArray());
                tree.addFile(name, ObjectStorageTypes.BLOB.save(blob, repository));
            }
        }

        // Save the tree
        return ObjectStorageTypes.TREE.save(tree, repository);
    }
    @Override
    public void close() throws IOException {
        this.ftp.disconnect();
    }
}
