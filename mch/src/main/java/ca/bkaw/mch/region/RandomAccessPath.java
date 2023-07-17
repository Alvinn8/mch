package ca.bkaw.mch.region;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An object that opens a {@link Path} for random access.
 * <p>
 * In case the path is a file a {@link RandomAccessFile} is used. In other cases
 * the file content is read in its entirety and used in a {@link ByteBuffer}.
 */
public interface RandomAccessPath extends Closeable {
    /**
     * Set the cursor offset, measured from the start of the file.
     *
     * @param pos The offset from the start of the file.
     * @throws IOException If an I/O error occurs.
     */
    void seek(int pos) throws IOException;

    /**
     * Read a signed 32-bit integer from the current cursor offset and increment the
     * cursor.
     *
     * @return The read integer.
     * @throws IOException  If an I/O error occurs.
     */
    int readInt() throws IOException;

    /**
     * Read a signed byte from the current cursor offset and increment the cursor.
     *
     * @return The read byte.
     * @throws IOException If an I/O error occurs.
     */
    byte readByte() throws IOException;

    /**
     * Read {@code bytes.length} bytes into the byte array and increment the cursor.
     *
     * @param bytes The byte array to read into.
     * @throws IOException If an I/O error occurs.
     */
    void readFully(byte[] bytes) throws IOException;

    /**
     * Open a path for random access.
     *
     * @param path The path.
     * @return The {@link RandomAccessPath} object.
     * @throws IOException If an I/O error occurs.
     */
    static RandomAccessPath of(Path path) throws IOException {
        try {
            return new RandomAccessFileImpl(path.toFile());
        } catch (UnsupportedOperationException e) {
            return new RandomAccessPathImpl(path);
        }
    }

    /**
     * An implementation of {@link RandomAccessPath} used when the path is a file that
     * can be opened for random access.
     */
    class RandomAccessFileImpl implements RandomAccessPath {
        private final RandomAccessFile file;

        public RandomAccessFileImpl(File file) throws FileNotFoundException {
            this.file = new RandomAccessFile(file, "r");
        }

        @Override
        public void seek(int pos) throws IOException {
            this.file.seek(pos);
        }

        @Override
        public int readInt() throws IOException {
            return this.file.readInt();
        }

        @Override
        public byte readByte() throws IOException {
            return this.file.readByte();
        }

        @Override
        public void readFully(byte[] bytes) throws IOException {
            this.file.readFully(bytes);
        }

        @Override
        public void close() throws IOException {
            this.file.close();
        }
    }

    /**
     * An implementation of {@link RandomAccessPath} used when the path is not a file.
     * The path will be read in its entirety.
     */
    class RandomAccessPathImpl implements RandomAccessPath {
        private final ByteBuffer byteBuffer;

        public RandomAccessPathImpl(Path path) throws IOException {
            try (InputStream stream = Files.newInputStream(path)) {
                byte[] bytes = stream.readAllBytes();
                this.byteBuffer = ByteBuffer.wrap(bytes);
            }
        }

        @Override
        public void seek(int pos) {
            this.byteBuffer.position(pos);
        }

        @Override
        public int readInt() {
            return this.byteBuffer.getInt();
        }

        @Override
        public byte readByte() {
            return this.byteBuffer.get();
        }

        @Override
        public void readFully(byte[] bytes) {
            this.byteBuffer.get(bytes);
        }

        @Override
        public void close() {

        }
    }
}
