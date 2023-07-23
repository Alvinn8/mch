package ca.bkaw.mch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * A SHA-1 hash.
 */
public class Sha1 {
    public static final String HEX_DIGITS = "0123456789abcdef";

    private final byte[] bytes;

    public Sha1(byte[] bytes) {
        this.bytes = bytes;
        if (this.bytes.length != 20) {
            throw new IllegalArgumentException("Sha1 hash must be 20 bytes, got " + bytes.length);
        }
    }

    /**
     * Get the SHA-1 hash from a hexadecimal string representation.
     *
     * @param hash The hexadecimal SHA-1 hash.
     * @return The SHA-1 hash.
     */
    public static Sha1 fromString(String hash) {
        if (hash.length() != 40) {
            throw new IllegalArgumentException("A hexadecimal hash string must be 40 characters.");
        }
        byte[] bytes = new byte[20];
        for (int i = 0; i < 40; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hash.charAt(i), 16) << 4)
                + Character.digit(hash.charAt(i+1), 16));
        }
        return new Sha1(bytes);
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the SHA-1 hash of a file.
     *
     * @param path The path to the file.
     * @return The SHA-1 hash.
     * @throws IOException If an I/O error occurs.
     */
    public static Sha1 ofFile(Path path) throws IOException {
        MessageDigest md = getMessageDigest();
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int i;
            while ((i = stream.read(buffer)) != -1) {
                md.update(buffer, 0, i);
            }
            byte[] digest = md.digest();
            return new Sha1(digest);
        }
    }

    /**
     * Get the SHA-1 hash of the information about a file.
     * <p>
     * Will use the file name, size and last modification date.
     *
     * @param path The path to the file.
     * @return The SHA-1 hash.
     * @throws IOException If an I/O error occurs.
     */
    @Deprecated
    public static Sha1 ofFileMeta(Path path) throws IOException {
        long size = Files.size(path);
        long lastModified = Files.getLastModifiedTime(path).toMillis();
        MessageDigest md = getMessageDigest();
        md.update(path.getFileName().toString().getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2);
        buffer.putLong(size);
        buffer.putLong(lastModified);
        md.update(buffer);
        return new Sha1(md.digest());
    }

    public static Sha1 of(String a, String b, Sha1 c) {
        MessageDigest md = getMessageDigest();
        md.update(a.getBytes(StandardCharsets.UTF_8));
        md.update(b.getBytes(StandardCharsets.UTF_8));
        md.update(c.getBytes());
        return new Sha1(md.digest());
    }

    /**
     * Create a random SHA-1 hash.
     *
     * @return The random SHA-1 hash.
     */
    public static Sha1 randomSha1() {
        byte[] bytes = new byte[20];
        try {
            SecureRandom.getInstanceStrong().nextBytes(bytes);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen. Every Java implementation is required to have at least
            // one implementation.
            throw new RuntimeException(e);
        }
        return new Sha1(bytes);
    }

    /**
     * Get the hexadecimal representation of this SHA-1 hash.
     *
     * @return The hexadecimal string (40 characters).
     */
    public String asHex() {
        StringBuilder str = new StringBuilder(2 * this.bytes.length);
        for (byte b : this.bytes) {
            char firstChar = HEX_DIGITS.charAt((b & 0xF0) >> 4);
            char secondChar = HEX_DIGITS.charAt((b & 0x0F));
            str.append(firstChar).append(secondChar);
        }
        return str.toString();
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;

        Sha1 sha1 = (Sha1) other;

        return Arrays.equals(this.bytes, sha1.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    @Override
    public String toString() {
        return "Sha1{" + this.asHex() + '}';
    }
}
