package ca.bkaw.mch.bukkit;

import ca.bkaw.mch.bukkit.command.MchCommand;
import ca.bkaw.mch.repository.MchRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;

public final class MchBukkit extends JavaPlugin {

    private final MchRepository repository = new MchRepository(Path.of("mch"));

    @Override
    public void onEnable() {
        try {
            this.repository.createDirectories();
        } catch (IOException e) {
            this.getLogger().severe("Failed to create directories.");
            e.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand command = this.getCommand("mch");
        if (command == null) {
            throw new RuntimeException("mch command not found");
        }
        command.setExecutor(new MchCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public MchRepository getRepository() {
        return this.repository;
    }
}
