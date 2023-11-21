package ca.bkaw.mch.cli.ftp;

import picocli.CommandLine.Command;

@Command(name = "ftp", subcommands = {
    AddFtpProfileCommand.class,
    EditFtpProfileCommand.class,
    ListFtpProfilesCommand.class,
    RemoveFtpProfileCommand.class,
})
public class FtpCommand {
}
