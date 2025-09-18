package me.petoma21.goodSign;
import org.bukkit.plugin.java.JavaPlugin;

public class GoodSign extends JavaPlugin {

    private GoodSignManager signManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        signManager = new GoodSignManager(this);
        RankingGUI rankingGUI = new RankingGUI(signManager);

        getServer().getPluginManager().registerEvents(new SignListener(this, signManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(rankingGUI, signManager), this);

        getCommand("gsign").setExecutor(new GoodSignCommand(signManager, configManager, rankingGUI));

        getLogger().info("&aGoodSign プラグインが有効になりました！");
    }

    @Override
    public void onDisable() {
        if (signManager != null) {
            signManager.saveData();
        }
        getLogger().info("&cGoodSign プラグインが無効になりました。");
    }

    public GoodSignManager getSignManager() {
        return signManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}