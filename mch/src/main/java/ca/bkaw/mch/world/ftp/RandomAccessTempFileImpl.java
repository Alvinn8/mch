package ca.bkaw.mch.world.ftp;

import ca.bkaw.mch.util.RandomAccessReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link ca.bkaw.mch.util.RandomAccessReader.RandomAccessFileImpl} that is used
 * on a temporary file.
 */
public class RandomAccessTempFileImpl extends RandomAccessReader.RandomAccessFileImpl {
    private final Path path;

    public RandomAccessTempFileImpl(File file) throws FileNotFoundException {
        super(file);
        path = file.toPath();
    }

    @Override
    public void close() throws IOException {
        super.close();
        Files.deleteIfExists(this.path);
    }
}
