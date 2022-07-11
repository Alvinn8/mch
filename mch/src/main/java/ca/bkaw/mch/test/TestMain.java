package ca.bkaw.mch.test;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.repository.MchRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class TestMain {
    public static void main(String[] args) throws IOException {
        MchRepository repository = new MchRepository(Path.of("."));

        HashMap<String, Reference20> dimensions = new HashMap<>();
        dimensions.put("overworld", new Reference20(new Sha1(new byte[20])));

        Commit commit = new Commit("Test commit", System.currentTimeMillis(), dimensions);

        ObjectStorageTypes.COMMIT.save(commit, repository);
    }
}
