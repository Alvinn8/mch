package ca.bkaw.mch;

public class MchVersion {
    public static final int VERSION_NUMBER = 2;

    private MchVersion() {}

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
