package me.petoma21.goodSign;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final GoodSign plugin;
    private FileConfiguration config;

    public ConfigManager(GoodSign plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public int getMaxSignsPerUser() {
        return config.getInt("max-signs-per-user", 5);
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}
