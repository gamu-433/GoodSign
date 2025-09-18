package me.petoma21.goodSign;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GUIListener implements Listener {

    private final RankingGUI rankingGUI;
    private final GoodSignManager signManager;

    public GUIListener(RankingGUI rankingGUI, GoodSignManager signManager) {
        this.rankingGUI = rankingGUI;
        this.signManager = signManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // ランキングGUIかどうかチェック
        String title = event.getView().getTitle();
        if (!title.contains("いいねランキング")) {
            return;
        }

        // クリックをキャンセル（アイテムの移動を防ぐ）
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // クリックされたアイテムの種類で処理を分岐
        switch (clickedItem.getType()) {
            case BARRIER:
                // GUIを閉じる
                player.closeInventory();
                player.sendMessage(formatMessage("&aGUIを閉じました。"));
                break;

            case CLOCK:
                // ランキング更新
                rankingGUI.openRankingGUI(player);
                player.sendMessage(formatMessage("&aランキングを更新しました！"));
                break;

            case GOLD_INGOT:
            case IRON_INGOT:
            case COPPER_INGOT:
            case PLAYER_HEAD:
                // プレイヤー詳細表示
                handlePlayerDetailClick(player, clickedItem);
                break;

            default:
                // 装飾アイテムなど、何もしない
                break;
        }
    }

    private void handlePlayerDetailClick(Player player, ItemStack clickedItem) {
        String displayName = clickedItem.getItemMeta().getDisplayName();

        // プレイヤー名を抽出
        String playerName = extractPlayerName(displayName);
        if (playerName == null) {
            return;
        }

        // そのプレイヤーの看板一覧を表示
        List<GoodSignData> userSigns = signManager.getUserSigns(playerName);

        if (userSigns.isEmpty()) {
            player.sendMessage(formatMessage("&c" + playerName + " の看板が見つかりませんでした。"));
            return;
        }

        // チャットで詳細情報を表示
        player.sendMessage(formatMessage("&a=== " + playerName + " の看板詳細 ==="));

        int totalLikes = 0;
        for (int i = 0; i < userSigns.size(); i++) {
            GoodSignData signData = userSigns.get(i);
            totalLikes += signData.getLikes();

            player.sendMessage(formatMessage("&e" + (i + 1) + ". &f" + signData.getTitle()));
            player.sendMessage(formatMessage("   &7場所: " + signData.getLocationString()));
            player.sendMessage(formatMessage("   &7いいね数: &c" + signData.getLikes()));

            // 10個を超える場合は省略
            if (i >= 9 && userSigns.size() > 10) {
                int remaining = userSigns.size() - 10;
                player.sendMessage(formatMessage("&7... 他 " + remaining + " 個"));
                break;
            }
        }

        player.sendMessage(formatMessage("&a総計: 看板数 " + userSigns.size() + "個, いいね数 " + totalLikes + "個"));
        player.sendMessage(formatMessage("&a平均: " + String.format("%.1f", (double) totalLikes / userSigns.size()) + " いいね/看板"));
    }

    private String extractPlayerName(String displayName) {
        try {
            // "第X位: プレイヤー名" の形式からプレイヤー名を抽出
            String cleanName = displayName.replaceAll("§[0-9a-fk-or]", ""); // カラーコード除去
            String[] parts = cleanName.split(": ");
            if (parts.length >= 2) {
                return parts[1].trim();
            }
        } catch (Exception e) {
            // 抽出失敗
        }
        return null;
    }

    private String formatMessage(String message) {
        return message.replace("&", "§");
    }
}
