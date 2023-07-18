package ca.bkaw.mch;

/**
 * An exception thrown when the mch version found is not supported.
 */
public class UnsupportedMchVersionException extends RuntimeException {
    public UnsupportedMchVersionException(String message) {
        super(message);
    }
}
