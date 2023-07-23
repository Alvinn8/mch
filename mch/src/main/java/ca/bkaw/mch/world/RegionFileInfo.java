package ca.bkaw.mch.world;

/**
 * Information about a region file.
 *
 * @param fileName The file name.
 * @param lastModified The last modified time of the region file.
 */
public record RegionFileInfo(String fileName, long lastModified) {
    private int getRegionCoordinate(int index) {
        String str = this.fileName.substring("r.".length(), this.fileName.length() - ".mca".length());
        return Integer.parseInt(str.split("\\.")[index]);
    }

    public int getRegionX() {
        return this.getRegionCoordinate(0);
    }

    public int getRegionZ() {
        return this.getRegionCoordinate(1);
    }
}
