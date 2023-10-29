package ca.bkaw.mch.world.zip;

import ca.bkaw.mch.world.DirectWorldProvider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * A {@link ca.bkaw.mch.world.WorldProvider} that reads files from a zip file.
 */
public class ZipWorldProvider extends DirectWorldProvider {

    public ZipWorldProvider(Path zipPath, String pathInsideZip) throws IOException {
        super(openZip(zipPath, pathInsideZip));
    }

    private static Path openZip(Path zipPath, String pathInsideZip) throws IOException {
        FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), new HashMap<>());
        return fileSystem.getPath(pathInsideZip);
    }

    @Override
    public void close() throws IOException {
        this.path.getFileSystem().close();
    }
}
