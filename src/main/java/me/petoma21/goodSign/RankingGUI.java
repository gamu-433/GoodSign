package me.petoma21.goodSign;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class RankingGUI {

    private final GoodSignManager signManager;

    public RankingGUI(GoodSignManager signManager) {
        this.signManager = signManager;
    }

    public void openRankingGUI(Player player) {
        // ランキングデータを取得
        List<PlayerRankingData> rankingData = calculateRanking();

        // GUIのサイズを決定（最大54スロット）
        int guiSize = Math.min(54, Math.max(9, ((rankingData.size() + 8) / 9) * 9));

        Inventory gui = Bukkit.createInventory(null, guiSize, formatMessage("&6&lいいねランキング"));

        // ランキングアイテムを追加
        for (int i = 0; i < Math.min(rankingData.size(), guiSize - 9); i++) {
            PlayerRankingData data = rankingData.get(i);
            ItemStack item = createRankingItem(data, i + 1);
            gui.setItem(i, item);
        }

        // 下部に装飾アイテムを追加
        addDecorationItems(gui, guiSize);

        player.openInventory(gui);
    }

    private List<PlayerRankingData> calculateRanking() {
        Map<String, Integer> playerTotalLikes = new HashMap<>();
        Map<String, Integer> playerSignCount = new HashMap<>();

        // 全ての看板データからプレイヤー別のいいね数を集計
        Collection<GoodSignData> allSigns = signManager.getAllGoodSigns();

        for (GoodSignData signData : allSigns) {
            String ownerName = signData.getOwnerName();
            int likes = signData.getLikes();

            playerTotalLikes.put(ownerName, playerTotalLikes.getOrDefault(ownerName, 0) + likes);
            playerSignCount.put(ownerName, playerSignCount.getOrDefault(ownerName, 0) + 1);
        }

        // ランキングデータリストを作成
        List<PlayerRankingData> rankingList = new ArrayList<>();
        for (String playerName : playerTotalLikes.keySet()) {
            int totalLikes = playerTotalLikes.get(playerName);
            int signCount = playerSignCount.get(playerName);
            rankingList.add(new PlayerRankingData(playerName, totalLikes, signCount));
        }

        // いいね数で降順ソート
        rankingList.sort((a, b) -> Integer.compare(b.getTotalLikes(), a.getTotalLikes()));

        return rankingList;
    }

    private ItemStack createRankingItem(PlayerRankingData data, int rank) {
        ItemStack item;

        // 1-3位は特別なアイテム、それ以外はプレイヤーヘッド
        switch (rank) {
            case 1:
                item = new ItemStack(Material.GOLD_INGOT);
                break;
            case 2:
                item = new ItemStack(Material.IRON_INGOT);
                break;
            case 3:
                item = new ItemStack(Material.COPPER_INGOT);
                break;
            default:
                item = new ItemStack(Material.PLAYER_HEAD);
                break;
        }

        ItemMeta meta = item.getItemMeta();

        // プレイヤーヘッドの場合、スキンを設定
        if (item.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            skullMeta.setOwner(data.getPlayerName());
        }

        // アイテム名を設定
        String rankColor = getRankColor(rank);
        meta.setDisplayName(formatMessage(rankColor + "&l第" + rank + "位: " + data.getPlayerName()));

        // 詳細情報をLoreに設定
        List<String> lore = new ArrayList<>();
        lore.add(formatMessage("&e総いいね数: &c" + data.getTotalLikes()));
        lore.add(formatMessage("&e看板数: &a" + data.getSignCount()));
        lore.add(formatMessage("&e平均いいね: &b" + String.format("%.1f", data.getAverageLikes())));
        lore.add("");
        lore.add(formatMessage("&7クリックで詳細を表示"));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private String getRankColor(int rank) {
        switch (rank) {
            case 1: return "&6"; // 金色
            case 2: return "&7"; // 銀色
            case 3: return "&c"; // 銅色
            default: return "&f"; // 白色
        }
    }

    private void addDecorationItems(Inventory gui, int guiSize) {
        // 最下段に装飾アイテムを配置
        int lastRowStart = guiSize - 9;

        // 閉じるボタン
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(formatMessage("&c&lGUIを閉じる"));
        closeItem.setItemMeta(closeMeta);
        gui.setItem(lastRowStart + 4, closeItem);

        // 更新ボタン
        ItemStack refreshItem = new ItemStack(Material.CLOCK);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName(formatMessage("&a&lランキング更新"));
        List<String> refreshLore = new ArrayList<>();
        refreshLore.add(formatMessage("&7クリックでランキングを更新"));
        refreshMeta.setLore(refreshLore);
        refreshItem.setItemMeta(refreshMeta);
        gui.setItem(lastRowStart + 8, refreshItem);

        // 装飾用ガラス
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = lastRowStart; i < guiSize; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glass);
            }
        }
    }

    private String formatMessage(String message) {
        return message.replace("&", "§");
    }

    // プレイヤーランキングデータクラス
    public static class PlayerRankingData {
        private final String playerName;
        private final int totalLikes;
        private final int signCount;

        public PlayerRankingData(String playerName, int totalLikes, int signCount) {
            this.playerName = playerName;
            this.totalLikes = totalLikes;
            this.signCount = signCount;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getTotalLikes() {
            return totalLikes;
        }

        public int getSignCount() {
            return signCount;
        }

        public double getAverageLikes() {
            return signCount > 0 ? (double) totalLikes / signCount : 0.0;
        }
    }
}
