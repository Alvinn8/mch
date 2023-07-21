package ca.bkaw.mch.bukkit.command;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.bukkit.MchBukkit;
import ca.bkaw.mch.command.CommitCommand;
import ca.bkaw.mch.object.ObjectNotFoundException;
import ca.bkaw.mch.object.ObjectStorageType;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.StorageObject;
import ca.bkaw.mch.repository.MchRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MchCommand implements CommandExecutor {
    private final MchBukkit plugin;

    public MchCommand(MchBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 0) {
            return this.version(sender);
        }
        switch (args[0]) {
            case "version" -> {
                return this.version(sender);
            }
            case "commit" -> {
                return this.commit(sender, args);
            }
            case "cat-file", "cat" -> {
                return this.cat(sender, args);
            }
        }
        return false;
    }

    private boolean version(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text(
            "mch version " + this.plugin.getDescription().getVersion()
        ));
        return true;
    }

    private boolean commit(@NotNull CommandSender sender, @NotNull String[] args) {
        sender.sendPlainMessage("Committing...");

        ArrayList<String> argsList = new ArrayList<>(List.of(args));
        argsList.remove(0);
        String commitMessage = String.join(" ", argsList);

        MchRepository repository = this.plugin.getRepository();

        try {
            CommitCommand.run(repository, commitMessage);
        } catch (IOException e) {
            sender.sendRichMessage("<red>Failed to commit: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean cat(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            return false;
        }
        String typeId = args[1];
        String hash = args[2];

        ObjectStorageType<?> type = ObjectStorageTypes.getType(typeId);

        if (type == null) {
            sender.sendMessage(Component.text(typeId + " is not an object storage type.", NamedTextColor.RED));
            return true;
        }

        if (hash.length() != 40) {
            sender.sendRichMessage("<red>Please specify the 40-character-hexadecimal SHA-1 hash of the object.");
            return true;
        }

        Sha1 sha1 = Sha1.fromString(hash);

        MchRepository repository = this.plugin.getRepository();

        StorageObject storageObject;
        try {
            storageObject = type.read(sha1, repository);
        } catch(ObjectNotFoundException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            return true;
        } catch (Exception e) {
            sender.sendRichMessage("<red>Failed to read object: " + e.getMessage());
            e.printStackTrace();
            return true;
        }

        sender.sendPlainMessage(typeId + " " + sha1.asHex() + ":\n" + storageObject.cat());

        return true;
    }
}
