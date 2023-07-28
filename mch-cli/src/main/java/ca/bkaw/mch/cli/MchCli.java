package ca.bkaw.mch.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "mch", subcommands = {
    AddWorldCommand.class,
    CommitCommand.class,
    CatCommand.class,
    InitCommand.class,
    LogCommand.class
})
public class MchCli {
    public static void main(String[] args) {
        int exitCode = new CommandLine(MchCli.class, new GuiceFactory()).execute(args);
        System.exit(exitCode);
    }
}
