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

        // [goodsign] の形式をチェック
        if (lines[0] != null && lines[0].equalsIgnoreCase("[goodsign]")) {

            // 権限チェック
            if (!player.hasPermission("goodsign.create")) {
                player.sendMessage(formatMessage("&cいいね看板を作成する権限がありません！"));
                event.setCancelled(true);
                return;
            }

            String title = lines[1];
            String ownerName = lines[2];

            // 入力値チェック
            if (title == null || title.trim().isEmpty()) {
                player.sendMessage(formatMessage("&cタイトルを入力してください！"));
                event.setCancelled(true);
                return;
            }

            if (ownerName == null || ownerName.trim().isEmpty()) {
                player.sendMessage(formatMessage("&cユーザー名を入力してください！"));
                event.setCancelled(true);
                return;
            }

            // いいね看板の作成
            if (signManager.createGoodSign(event.getBlock().getLocation(), title, ownerName, player)) {
                event.setCancelled(true); // 元の看板編集をキャンセル
            } else {
                player.sendMessage(formatMessage("&cいいね看板の作成に失敗しました。"));
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

        // 右クリックの場合
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            if (signManager.isGoodSign(block.getLocation())) {
                // いいね看板の場合、編集を無効にしていいね処理
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
            // いいね看板の破壊権限チェック
            if (!signManager.canBreakSign(block.getLocation(), player)) {
                player.sendMessage(formatMessage("&cこのいいね看板を破壊する権限がありません！"));
                event.setCancelled(true);
                return;
            }

            // いいね看板データの削除
            signManager.removeGoodSign(block.getLocation());
            player.sendMessage(formatMessage("&aいいね看板を削除しました。"));
        }
    }

    // 看板を保護するためのイベント
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        if (signManager.isGoodSign(block.getLocation())) {
            // いいね看板の物理変化をキャンセル
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(org.bukkit.event.block.BlockFromToEvent event) {
        Block toBlock = event.getToBlock();
        if (toBlock.getState() instanceof Sign && signManager.isGoodSign(toBlock.getLocation())) {
            // いいね看板への水流をキャンセル
            event.setCancelled(true);
        }
    }

    private String formatMessage(String message) {
        return message.replace("&", "§");
    }
}