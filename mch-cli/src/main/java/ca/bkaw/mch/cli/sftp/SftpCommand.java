package ca.bkaw.mch.cli.sftp;


import picocli.CommandLine;

@CommandLine.Command(name = "sftp", subcommands = {
    AddSftpProfileCommand.class,
    EditSftpProfileCommand.class,
    ListSftpProfilesCommand.class,
    RemoveSftpProfileCommand.class,
})
public class SftpCommand {
}
