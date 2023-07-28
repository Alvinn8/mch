package ca.bkaw.mch.cli;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.ObjectNotFoundException;
import ca.bkaw.mch.object.ObjectStorageType;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.repository.MchRepository;
import com.google.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "cat", aliases = "cat-file")
public class CatCommand implements Callable<Integer> {
    @Inject
    MchRepository repository;

    @Parameters(index = "0", paramLabel = "type id")
    String typeId;

    @Parameters(index = "1", paramLabel = "hash")
    String hash;

    @Override
    public Integer call() {
        ObjectStorageType<?> type = ObjectStorageTypes.getType(typeId);

        if (type == null) {
            System.err.println(typeId + " is not an object storage type.");
            return ExitCode.USAGE;
        }

        if (hash.length() != 40) {
            System.err.println("Please specify the 40-character-hexadecimal SHA-1 hash of the object.");
            return ExitCode.USAGE;
        }

        Sha1 sha1 = Sha1.fromString(hash);

        StorageObject storageObject;
        try {
            storageObject = type.read(sha1, repository);
        } catch (ObjectNotFoundException e) {
            System.err.println(e.getMessage());
            return ExitCode.SOFTWARE;
        } catch (Exception e) {
            System.err.println("Failed to read object: " + e.getMessage());
            e.printStackTrace();
            return ExitCode.SOFTWARE;
        }

        System.out.println(typeId + " " + sha1.asHex() + ":\n" + storageObject.cat());
        return ExitCode.OK;
    }
}
