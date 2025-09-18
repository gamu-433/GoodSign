package me.petoma21.goodSign;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GoodSignManager {

    private final JavaPlugin plugin;
    private final Map<String, GoodSignData> goodSigns;
    private final Map<String, Set<String>> playerLikes;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    public GoodSignManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.goodSigns = new HashMap<>();
        this.playerLikes = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "goodsigns.yml");

        loadData();
    }

    public boolean createGoodSign(Location location, String title, String ownerName, Player creator) {
        String locationKey = getLocationKey(location);

        if (goodSigns.containsKey(locationKey)) {
            return false;
        }

        // 権限チェック（OP以外の場合）
        if (!creator.isOp()) {
            ConfigManager config = new ConfigManager((GoodSign) plugin);
            int maxSigns = config.getMaxSignsPerUser();
            int currentSigns = getUserSignCount(creator.getName());

            if (currentSigns >= maxSigns) {
                creator.sendMessage(formatMessage("&c最大" + maxSigns + "個までしかいいね看板を作成できません！"));
                return false;
            }
        }

        Block block = location.getBlock();
        if (!(block.getState() instanceof Sign)) {
            return false;
        }

        GoodSignData signData = new GoodSignData(location, title, ownerName, creator.getName(), 0);
        goodSigns.put(locationKey, signData);

// ここで plugin, signData などを final 変数にして Runnable で使えるようにする
        final GoodSignData finalSignData = signData;
        final Block finalBlock = block;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!(finalBlock.getState() instanceof Sign)) {
                    return;
                }
                Sign sign = (Sign) finalBlock.getState();

                // 看板の明度を判定
                String textColor = getTextColorForSign(sign);

                sign.setLine(0, formatMessage("&a&l" + finalSignData.getTitle()));
                sign.setLine(1, formatMessage(textColor + "&l" + finalSignData.getOwnerName()));
                sign.setLine(2, "");
                sign.setLine(3, formatMessage("&c&l0 " + textColor + "&lいいね"));

                sign.update();
                saveData();
            }
        }.runTaskLater(plugin, 5L);

        creator.sendMessage(formatMessage("&aいいね看板を作成しました！"));
        return true;

    }

    public boolean addLike(Location location, Player player) {
        String locationKey = getLocationKey(location);
        GoodSignData signData = goodSigns.get(locationKey);

        if (signData == null) {
            return false;
        }

        String playerName = player.getName();
        Set<String> likedSigns = playerLikes.getOrDefault(playerName, new HashSet<>());

        if (likedSigns.contains(locationKey)) {
            player.sendMessage(formatMessage("&c既にいいねしているため、できません！"));
            return false;
        }

        signData.addLike();
        likedSigns.add(locationKey);
        playerLikes.put(playerName, likedSigns);

        updateSignDisplay(signData);
        saveData();

        player.sendMessage(formatMessage("&a" + signData.getOwnerName() + " さんにいいねをしました！"));

        // いいね成功時にレベルアップ音を再生
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        return true;
    }

    public boolean canBreakSign(Location location, Player player) {
        String locationKey = getLocationKey(location);
        GoodSignData signData = goodSigns.get(locationKey);

        if (signData == null) {
            return true; // 通常の看板
        }

        // 作成者またはOP権限のみ破壊可能
        return player.isOp() || player.getName().equals(signData.getCreator());
    }

    public void removeGoodSign(Location location) {
        String locationKey = getLocationKey(location);
        goodSigns.remove(locationKey);

        // プレイヤーのいいね履歴からも削除
        for (Set<String> likedSigns : playerLikes.values()) {
            likedSigns.remove(locationKey);
        }

        saveData();
    }

    public boolean isGoodSign(Location location) {
        return goodSigns.containsKey(getLocationKey(location));
    }

    public List<GoodSignData> getUserSigns(String username) {
        List<GoodSignData> userSigns = new ArrayList<>();
        for (GoodSignData signData : goodSigns.values()) {
            if (signData.getOwnerName().equals(username)) {
                userSigns.add(signData);
            }
        }
        return userSigns;
    }

    public Collection<GoodSignData> getAllGoodSigns() {
        return goodSigns.values();
    }

    private int getUserSignCount(String creatorName) {
        int count = 0;
        for (GoodSignData signData : goodSigns.values()) {
            if (signData.getCreator().equals(creatorName)) {
                count++;
            }
        }
        return count;
    }

    private void updateSignDisplay(GoodSignData signData) {
        Block block = signData.getLocation().getBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) block.getState();

        // 看板の明度を判定
        String textColor = getTextColorForSign(sign);

        sign.setLine(0, formatMessage("&a&l" + signData.getTitle()));
        sign.setLine(1, formatMessage(textColor + "&l" + signData.getOwnerName()));
        sign.setLine(2, "");
        sign.setLine(3, formatMessage("&c&l" + signData.getLikes() + "  " + textColor + "&lいいね"));

        sign.update();
    }

    private String getTextColorForSign(Sign sign) {
        Material material = sign.getBlock().getType();

        // 暗い色の看板の場合は白文字、明るい色の場合は黒文字
        switch (material) {
            case DARK_OAK_SIGN:
            case DARK_OAK_WALL_SIGN:
            case SPRUCE_SIGN:
            case SPRUCE_WALL_SIGN:
            case CRIMSON_SIGN:
            case CRIMSON_WALL_SIGN:
            case WARPED_SIGN:
            case WARPED_WALL_SIGN:
                return "&f";
            default:
                return "&0";
        }
    }

    private String getLocationKey(Location location) {
        return location.getWorld().getName() + "_" +
                location.getBlockX() + "_" +
                location.getBlockY() + "_" +
                location.getBlockZ();
    }

    private String formatMessage(String message) {
        return message.replace("&", "§");
    }

    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // 看板データの読み込み
        if (dataConfig.contains("signs")) {
            for (String key : dataConfig.getConfigurationSection("signs").getKeys(false)) {
                try {
                    GoodSignData signData = GoodSignData.fromConfig(dataConfig.getConfigurationSection("signs." + key));
                    if (signData != null) {
                        goodSigns.put(key, signData);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("看板データの読み込みに失敗しました: " + key);
                }
            }
        }

        // いいね履歴の読み込み
        if (dataConfig.contains("player_likes")) {
            for (String playerName : dataConfig.getConfigurationSection("player_likes").getKeys(false)) {
                List<String> likedSigns = dataConfig.getStringList("player_likes." + playerName);
                playerLikes.put(playerName, new HashSet<>(likedSigns));
            }
        }
    }

    public void saveData() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }

        // 看板データの保存
        dataConfig.set("signs", null);
        for (Map.Entry<String, GoodSignData> entry : goodSigns.entrySet()) {
            entry.getValue().saveToConfig(dataConfig, "signs." + entry.getKey());
        }

        // いいね履歴の保存
        dataConfig.set("player_likes", null);
        for (Map.Entry<String, Set<String>> entry : playerLikes.entrySet()) {
            dataConfig.set("player_likes." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("データの保存に失敗しました: " + e.getMessage());
        }
    }
}