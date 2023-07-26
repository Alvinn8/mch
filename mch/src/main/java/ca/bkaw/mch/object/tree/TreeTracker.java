package ca.bkaw.mch.object.tree;

import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.blob.Blob;
import ca.bkaw.mch.repository.MchRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A utility for tracking a directory of files and subdirectories.
 */
public class TreeTracker {
    /**
     * Track a directory of files by creating {@link Tree} and {@link Blob} objects to
     * represent the current version of the directory.
     *
     * @param repository The repository to save to.
     * @param directory The directory to track.
     * @param predicate A predicate that controls which files to include.
     * @return The reference to the created {@link Tree} object.
     * @throws IOException If an I/O error occurs.
     */
    public static Reference20<Tree> trackDirectoryTree(MchRepository repository, Path directory, Predicate<String> predicate) throws IOException {
        Tree tree = new Tree();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path path : files.sorted().toList()) {
                String name = path.getFileName().toString();
                if (Files.isDirectory(path)) {
                    // Track subdirectories
                    Reference20<Tree> subDirectoryReference = trackDirectoryTree(repository, path, str -> true);
                    tree.addSubTree(name, subDirectoryReference);
                }  else if (Files.isRegularFile(path)) {
                    // Track files
                    byte[] bytes = Files.readAllBytes(path);
                    Blob blob = new Blob(bytes);
                    tree.addFile(name, ObjectStorageTypes.BLOB.save(blob, repository));
                }
            }
        }

        // Save the tree
        return ObjectStorageTypes.TREE.save(tree, repository);
    }
}
