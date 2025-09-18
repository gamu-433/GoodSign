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
        String title = event.getView().getTitle();
        if (!title.contains("いいねランキング")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        switch (clickedItem.getType()) {
            case BARRIER:
                player.closeInventory();
                break;

            case CLOCK:
                rankingGUI.openRankingGUI(player);
                player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &a&lランキングを更新しました！"));
                break;

            case GOLD_INGOT:
            case IRON_INGOT:
            case COPPER_INGOT:
            case PLAYER_HEAD:
                handlePlayerDetailClick(player, clickedItem);
                break;

            default:
                break;
        }
    }

    private void handlePlayerDetailClick(Player player, ItemStack clickedItem) {
        String displayName = clickedItem.getItemMeta().getDisplayName();

        String playerName = extractPlayerName(displayName);
        if (playerName == null) {
            return;
        }

        List<GoodSignData> userSigns = signManager.getUserSigns(playerName);

        if (userSigns.isEmpty()) {
            player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &c&l" + playerName + " の看板が見つかりませんでした。"));
            return;
        }

        player.sendMessage(formatMessage("&a&l=== &6" + playerName + " &a&lの看板詳細 ==="));

        int totalLikes = 0;
        for (int i = 0; i < userSigns.size(); i++) {
            GoodSignData signData = userSigns.get(i);
            totalLikes += signData.getLikes();

            player.sendMessage(formatMessage("&e" + (i + 1) + ". &f" + signData.getTitle()));
            player.sendMessage(formatMessage("   &7場所: " + signData.getLocationString()));
            player.sendMessage(formatMessage("   &7いいね数: &c" + signData.getLikes()));

            if (i >= 9 && userSigns.size() > 10) {
                int remaining = userSigns.size() - 10;
                player.sendMessage(formatMessage("&7... 他 " + remaining + " 個"));
                break;
            }
        }

        player.sendMessage(formatMessage("&f総計: &a看板数 &f" + userSigns.size() + "&a個, &dいいね数 &f" + totalLikes + "&d個"));
        player.sendMessage(formatMessage("&f平均: " + String.format("%.1f", (double) totalLikes / userSigns.size()) + " いいね/看板"));
    }

    private String extractPlayerName(String displayName) {
        try {
            String cleanName = displayName.replaceAll("§[0-9a-fk-or]", ""); // カラーコード消す
            String[] parts = cleanName.split(": ");
            if (parts.length >= 2) {
                return parts[1].trim();
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String formatMessage(String message) {
        return message.replace("&", "§");
    }
}
