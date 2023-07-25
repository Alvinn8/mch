package ca.bkaw.mch.cli;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.command.CommitCommand;
import ca.bkaw.mch.object.ObjectNotFoundException;
import ca.bkaw.mch.object.ObjectStorageType;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.repository.MchConfiguration;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.repository.TrackedWorld;
import ca.bkaw.mch.world.DirectWorldProvider;
import ca.bkaw.mch.world.WorldProvider;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MchCli {
    public static void main(String[] args) {
        MchCli cli = new MchCli();
        if (args.length < 1) {
            printHelp();
            return;
        }

        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, args.length - 1);

        cli.runCommand(args[0], commandArgs);
    }

    public void runCommand(String command, String[] commandArgs) {
        switch (command) {
            case "add-world" -> addWorld(commandArgs);
            case "cat", "cat-file" -> cat(commandArgs);
            case "commit" -> commit(commandArgs);
            case "init" -> init();
            default -> printHelp();
        }
    }

    public static void printHelp() {
        System.out.println("mch commands");
        System.out.println();
        System.out.println("main commands");
        System.out.println("    commit      Save a snapshot of the tracked worlds.");
        System.out.println("    restore");
        System.out.println();
        System.out.println("configuration");
        System.out.println("    init        Create a new mch repository.");
        System.out.println("    add-world   Add a world to be tracked by this mch repository.");
        System.out.println();
        System.out.println("development commands");
        System.out.println("    cat         Print an object's content as human-readable text.");
    }

    public MchRepository findRepository() {
        MchRepository mchRepository = this.tryFindRepositoryInPath(Path.of(".").toAbsolutePath());
        if (mchRepository == null) {
            System.err.println("No mch repository found in this directory.");
            return null;
        }
        try {
            mchRepository.readConfiguration();
        } catch (IOException e) {
            System.err.println("Failed to read mch configuration");
            e.printStackTrace();
        }
        return mchRepository;
    }

    private MchRepository tryFindRepositoryInPath(Path path) {
        Path mchPath = path.resolve("mch");
        if (Files.isDirectory(mchPath)) {
            return new MchRepository(mchPath);
        }
        return null;
    }

    public void init() {
        MchRepository repository = new MchRepository(Path.of("mch"));
        try {
            repository.createDirectories();
            repository.readConfiguration();
            repository.saveConfiguration();
            repository.setHeadCommit(null);
        } catch (IOException e) {
            System.err.println("Failed to initialize mch repository.");
            e.printStackTrace();
            return;
        }
        System.out.println("Initialized a new mch repository.");
    }

    public void addWorld(String[] args) {
        MchRepository repository = this.findRepository();
        if (repository == null) {
            return;
        }

        MchConfiguration configuration = repository.getConfiguration();

        WorldProvider worldProvider = new DirectWorldProvider(Path.of(args[0]));

        Sha1 id = Sha1.randomSha1();
        TrackedWorld trackedWorld = new TrackedWorld(id, worldProvider);

        configuration.getTrackedWorlds().add(trackedWorld);

        try {
            repository.saveConfiguration();
        } catch (IOException e) {
            System.err.println("Failed to save configuration.");
            e.printStackTrace();
            return;
        }

        System.out.println("Now tracking world with id " + id.asHex());
    }

    public void commit(String[] args) {
        MchRepository repository = this.findRepository();
        if (repository == null) {
            return;
        }

        OptionParser optionParser = new OptionParser();
        optionParser.accepts("m");

        OptionSet optionSet = optionParser.parse(args);

        String commitMessage = (String) optionSet.valueOf("m");

        try {
            CommitCommand.run(repository, commitMessage);
        } catch (IOException e) {
            System.err.println("Failed to commit.");
            e.printStackTrace();
        }
    }


    public void cat(String[] args) {
        MchRepository repository = this.findRepository();
        if (repository == null) {
            return;
        }

        if (args.length < 2) {
            System.out.println("mch cat <type id> <hash>");
            return;
        }
        String typeId = args[0];
        String hash = args[1];

        ObjectStorageType<?> type = ObjectStorageTypes.getType(typeId);

        if (type == null) {
            System.err.println(typeId + " is not an object storage type.");
            System.exit(1);
        }

        if (hash.length() != 40) {
            System.err.println("Please specify the 40-character-hexadecimal SHA-1 hash of the object.");
            System.exit(1);
        }

        Sha1 sha1 = Sha1.fromString(hash);

        StorageObject storageObject;
        try {
            storageObject = type.read(sha1, repository);
        } catch (ObjectNotFoundException e) {
            System.err.println(e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Failed to read object: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println(typeId + " " + sha1.asHex() + ":\n" + storageObject.cat());
    }
}
