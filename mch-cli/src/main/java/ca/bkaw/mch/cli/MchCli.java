package ca.bkaw.mch.cli;

import ca.bkaw.mch.cli.ftp.FtpCommand;
import ca.bkaw.mch.cli.hub.HubCommand;
import ca.bkaw.mch.cli.research.ResearchCommand;
import ca.bkaw.mch.cli.sftp.SftpCommand;
import ca.bkaw.mch.cli.world.WorldCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.nio.file.Path;

@Command(name = "mch", subcommands = {
    CommitCommand.class,
    CatCommand.class,
    FtpCommand.class,
    HubCommand.class,
    InitCommand.class,
    LogCommand.class,
    ResearchCommand.class,
    RestoreCommand.class,
    SftpCommand.class,
    WorldCommand.class,
})
public class MchCli {
    public static void main(String[] args) {
        int exitCode = run(Path.of("."), args);
        System.exit(exitCode);
    }

    public static int run(Path workingDir, String[] args) {
        return new CommandLine(MchCli.class, new GuiceFactory(workingDir))
            .setTrimQuotes(false)
            .execute(args);
    }
}
