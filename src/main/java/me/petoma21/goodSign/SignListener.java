package me.petoma21.goodSign;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SignListener implements Listener {

    private final JavaPlugin plugin;
    private final GoodSignManager signManager;

    public SignListener(JavaPlugin plugin, GoodSignManager signManager) {
        this.plugin = plugin;
        this.signManager = signManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String[] lines = event.getLines();

        if (lines[0] != null && lines[0].equalsIgnoreCase("[goodsign]")) {

            if (!player.hasPermission("goodsign.create")) {
                player.sendMessage(formatMessage("&7[&c!&7] &c&lいいね看板を作成する権限がありません！"));
                event.setCancelled(true);
                return;
            }

            String title = lines[1];
            String ownerName = lines[2];

            if (title == null || title.trim().isEmpty()) {
                player.sendMessage(formatMessage("&7[&c!&7] &c&lタイトルを入力してください！"));
                event.setCancelled(true);
                return;
            }

            if (ownerName == null || ownerName.trim().isEmpty()) {
                player.sendMessage(formatMessage("&7[&c!&7] &c&lユーザー名を入力してください！"));
                event.setCancelled(true);
                return;
            }

            if (signManager.createGoodSign(event.getBlock().getLocation(), title, ownerName, player)) {
                player.sendMessage(formatMessage("&7[&b!&7] &a&lいいね看板を作成しました！"));
                event.setCancelled(true); // 元の看板編集をキャンセル
            } else {
                player.sendMessage(formatMessage("&7[&c!&7] &c&lいいね看板の作成に失敗しました。"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Player player = event.getPlayer();

        if (event.getAction().name().contains("RIGHT_CLICK")) {
            if (signManager.isGoodSign(block.getLocation())) {
                event.setCancelled(true);
                signManager.addLike(block.getLocation(), player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Player player = event.getPlayer();

        if (signManager.isGoodSign(block.getLocation())) {
            if (!signManager.canBreakSign(block.getLocation(), player)) {
                player.sendMessage(formatMessage("&7[&c!&7] &c&lこのいいね看板を破壊する権限がありません！"));
                event.setCancelled(true);
                return;
            }

            signManager.removeGoodSign(block.getLocation());
            player.sendMessage(formatMessage("&7[&b!&7] &a&lいいね看板を削除しました。"));
        }
    }

    private String formatMessage(String message) {
        return message.replace("&", "§");
    }
}
