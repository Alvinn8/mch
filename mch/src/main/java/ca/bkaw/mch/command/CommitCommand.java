package ca.bkaw.mch.command;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.object.dimension.Dimension;
import ca.bkaw.mch.object.regionfile.RegionFile;
import ca.bkaw.mch.object.regionfile.RegionFileChunk;
import ca.bkaw.mch.provider.Provider;
import ca.bkaw.mch.repository.MchRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommitCommand {
    public static void run(MchRepository repository, Provider provider, String commitMessage) throws IOException {

        Map<String, Reference20> dimensions = new HashMap<>();

        for (String dimensionName : provider.getDimensions()) {
            List<String> regionFileList = provider.getRegionFiles(dimensionName);
            Map<String, Reference20> regionFileMap = new HashMap<>();

            for (String regionFileName : regionFileList) {
                Sha1 contentHash = provider.getRegionFileHash(dimensionName, regionFileName);
                Sha1 identifierHash = RegionFile.getRegionFileIdentifier(dimensionName, regionFileName, contentHash);
                if (!ObjectStorageTypes.REGION_FILE.exists(identifierHash, repository)) {
                    ArrayList<RegionFileChunk> chunks = new ArrayList<>();
                    // todo
                    RegionFile regionFile = new RegionFile(identifierHash, chunks);
                    ObjectStorageTypes.REGION_FILE.save(regionFile, repository);
                }
                regionFileMap.put(regionFileName, new Reference20(identifierHash));
            }

            Dimension dimension = new Dimension(regionFileMap);
            Sha1 dimensionHash = ObjectStorageTypes.DIMENSION.save(dimension, repository);
            Reference20 dimensionRef = new Reference20(dimensionHash);
            dimensions.put(dimensionName, dimensionRef);
        }

        Commit commit = new Commit(commitMessage, System.currentTimeMillis(), dimensions);
        Sha1 commitHash = ObjectStorageTypes.COMMIT.save(commit, repository);

        System.out.println("Created commit " + commitHash.asHex());
    }
}
