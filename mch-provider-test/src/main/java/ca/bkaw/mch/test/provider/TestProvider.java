package ca.bkaw.mch.test.provider;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.regionfile.RegionFile;
import ca.bkaw.mch.provider.Provider;
import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class TestProvider implements Provider {
    private final Path worldFolder;

    public TestProvider(Path worldFolder) {
        this.worldFolder = worldFolder;
    }

    @Override
    public List<String> getDimensions() {
        return Collections.singletonList("overworld"); // TODO
    }

    @Override
    public List<String> getRegionFiles(String dimension) throws IOException {
        Path regionFolder = this.worldFolder.resolve(RegionFile.REGION_FOLDER);
        try (Stream<Path> stream = Files.list(regionFolder)) {
            return stream
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(path -> path.endsWith(".mca"))
                .toList();
        }
    }

    @Override
    public Sha1 getRegionFileHash(String dimension, String regionFile) throws IOException {
        Path regionFolder = this.worldFolder.resolve(RegionFile.REGION_FOLDER);
        Path regionFilePath = regionFolder.resolve(regionFile);
        return Sha1.ofFile(regionFilePath);
    }

    @Override
    public byte[] getChunkBytes(String dimension, String regionFile, int relativeChunkX, int relativeChunkY) throws IOException {
        Path regionFolder = this.worldFolder.resolve(RegionFile.REGION_FOLDER);
        Path regionFilePath = regionFolder.resolve(regionFile);
        MCAFile mcaFile = MCAUtil.read(regionFilePath.toFile());
        Chunk chunk = mcaFile.getChunk(relativeChunkX, relativeChunkY);

        return new byte[0];
    }
}
