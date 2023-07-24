package ca.bkaw.mch.cli;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.util.HashMap;
import java.util.Map;

public class MchCli {
    private final Map<String, Command> commands;

    public MchCli() {
        this.commands = new HashMap<>();
        this.registerCommand(MchCli::cat, "cat", "cat-file");
    }

    private void registerCommand(Command executor, String... aliases) {
        for (String alias : aliases) {
            this.commands.put(alias, executor);
        }
    }

    public static void main(String[] args) {
        MchCli cli = new MchCli();
        if (args.length < 1) {
            printHelp();
            return;
        }

        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 0, commandArgs, 0, args.length - 1);

        cli.runCommand(args[0], commandArgs);
    }

    public void runCommand(String command, String[] commandArgs) {
        Command commandExecutor = this.commands.get(command);
        if (commandExecutor == null) {
            printHelp();
            return;
        }
        commandExecutor.execute(commandArgs);
    }

    public static void printHelp() {
        System.out.println("mch commands");
        System.out.println();
        System.out.println("main commands");
        System.out.println("    commit - Save a snapshot of the tracked worlds.");
        System.out.println("    restore");
        System.out.println();
        System.out.println("configuration");
        System.out.println("    add-world - Add a world to be tracked by this mch repository.");
        System.out.println();
        System.out.println("development commands");
        System.out.println("    cat - Print an object's content as human-readable text.");
    }

    public static void cat(String[] args) {
        OptionParser optionParser = new OptionParser();
        OptionSet optionSet = optionParser.parse(args);
        String objectType = (String) optionSet.nonOptionArguments().get(0);
        System.out.println("objectType = " + objectType);
    }
}
