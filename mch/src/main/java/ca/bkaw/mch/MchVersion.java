package ca.bkaw.mch;

/**
 * Holds the current mch version and utility methods used to validate the version.
 */
public class MchVersion {
    /**
     * The version number used in object files to determine which version of mch it
     * was saved in.
     */
    public static final int VERSION_NUMBER = 3;

    private MchVersion() {}

    /**
     * Validate an mch version to ensure it is above the minimum version and that it
     * is not newer than the current version.
     *
     * @param mchVersion The mch version to validate.
     * @param minVersion The minimum supported version.
     * @throws UnsupportedMchVersionException If the mch version is not supported.
     */
    public static void validate(int mchVersion, int minVersion) {
        if (mchVersion < minVersion) {
            throw new UnsupportedMchVersionException(
                "Unsupported mch version " + mchVersion + " only " + minVersion
                    + " or higher is supported."
            );
        }
        if (mchVersion > VERSION_NUMBER) {
            throw new UnsupportedMchVersionException(
                "Future mch version number " + mchVersion + ", current is " +
                    VERSION_NUMBER + ". The repository seems to have been " +
                    "saved with a newer version of mch. Please update mch " +
                    "to interact with the repository."
            );
        }
    }
}
