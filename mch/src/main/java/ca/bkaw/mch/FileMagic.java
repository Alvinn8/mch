package ca.bkaw.mch;

import java.io.DataInput;
import java.io.IOException;

/**
 * A central place for all magic numbers used in mch file formats.
 */
public class FileMagic {
    public static final int CONFIGURATION = 0x6D6368_43; // mchC
    public static final int WORLD_CONTAINER = 0x6D6368_57; // mchW
    public static final int CHUNK_STORAGE = 0x6D6368_63; // mchc
    public static final int DIMENSION = 0x6D6368_64; // mchd
    public static final int COMMIT = 0x6D6368_6B; // mchk
    public static final int REGION_STORAGE = 0x6D6368_72; // mchr
    public static final int TREE = 0x6D6368_74; // mcht
    public static final int REGION_FILE = 0x6D6368_76; // mchv
    public static final int WORLD = 0x6D6368_77; // mchw

    public static void validate(DataInput dataInput, int magic) throws IOException {
        int found = dataInput.readInt();
        if (found != magic) {
            throw new RuntimeException("Expected magic " + Integer.toHexString(magic) + " but found " + Integer.toHexString(found) + ". Is the file corrupted?");
        }
    }
}
