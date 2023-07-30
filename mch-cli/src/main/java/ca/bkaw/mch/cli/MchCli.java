package ca.bkaw.mch.cli;

import ca.bkaw.mch.cli.world.WorldCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Arrays;

@Command(name = "mch", subcommands = {
    CommitCommand.class,
    CatCommand.class,
    InitCommand.class,
    LogCommand.class,
    RestoreCommand.class,
    WorldCommand.class,
})
public class MchCli {
    public static void main(String[] args) {
        System.out.println("args = " + Arrays.toString(args));
        int exitCode = new CommandLine(MchCli.class, new GuiceFactory())
            .setTrimQuotes(false)
            .execute(args);
        System.exit(exitCode);
    }
}
