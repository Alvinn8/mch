package ca.bkaw.mch.cli;

import ca.bkaw.mch.cli.ftp.FtpCommand;
import ca.bkaw.mch.cli.sftp.SftpCommand;
import ca.bkaw.mch.cli.world.WorldCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "mch", subcommands = {
    CommitCommand.class,
    CatCommand.class,
    FtpCommand.class,
    InitCommand.class,
    LogCommand.class,
    RestoreCommand.class,
    SftpCommand.class,
    WorldCommand.class,
})
public class MchCli {
    public static void main(String[] args) {
        int exitCode = new CommandLine(MchCli.class, new GuiceFactory())
            .setTrimQuotes(false)
            .execute(args);
        System.exit(exitCode);
    }
}
