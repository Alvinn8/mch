package ca.bkaw.mch.cli.world;

import picocli.CommandLine.Command;

@Command(name = "world", subcommands = {
    AddWorldCommand.class,
    EditWorldCommand.class,
    GetWorldCommand.class,
    RenameWorldCommand.class,
})
public class WorldCommand {
}
