package me.petoma21.goodSign;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignListener implements Listener {

    private final JavaPlugin plugin;
    private final GoodSignManager signManager;

    private final Map<UUID, SignCreationData> signCreationMap = new HashMap<>();

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
                player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l]　&c&lいいね看板を作成する権限がありません！"));
                event.setCancelled(true);
                return;
            }

            String title = lines[1];
            String ownerName = lines[2];

            if ((title == null || title.trim().isEmpty()) || (ownerName == null || ownerName.trim().isEmpty())) {
                event.setLine(0, "");
                event.setLine(1, "");
                event.setLine(2, "");
                event.setLine(3, "");

                Location signLocation = event.getBlock().getLocation();
                signCreationMap.put(player.getUniqueId(), new SignCreationData(signLocation, SignCreationStep.TITLE));

                player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &a&lいいね看板作成モードに入りました！"));
                player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &e&lチャットでタイトルを入力してください："));
                return;
            }

            if (signManager.createGoodSign(event.getBlock().getLocation(), title, ownerName, player)) {
                event.setCancelled(true);
            } else {
                player.sendMessage(formatMessage("&7[&c!&7] &c&lいいね看板の作成に失敗しました。"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!signCreationMap.containsKey(playerId)) {
            return;
        }

        event.setCancelled(true); // チャットキャンセル

        SignCreationData creationData = signCreationMap.get(playerId);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            signCreationMap.remove(playerId);
            player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &c&lいいね看板の作成をキャンセルしました。"));
            return;
        }

        switch (creationData.getStep()) {
            case TITLE:
                if (input.isEmpty()) {
                    player.sendMessage(formatMessage("&7[&c!&7] &c&lタイトルを入力してください（cancelでキャンセル）："));
                    return;
                }
                creationData.setTitle(input);
                creationData.setStep(SignCreationStep.OWNER);
                player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &a&lタイトル: &f" + input));
                player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &e&l次にMCIDを入力してください："));
                break;

            case OWNER:
                if (input.isEmpty()) {
                    player.sendMessage(formatMessage("&7[&c!&7] &c&lMCIDを入力してください（cancelでキャンセル）："));
                    return;
                }
                creationData.setOwnerName(input);

                Location signLocation = creationData.getSignLocation();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (signManager.createGoodSign(signLocation, creationData.getTitle(), creationData.getOwnerName(), player)) {
                        player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &a&lいいね看板が作成されました！"));
                        player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &a&lタイトル: &f" + creationData.getTitle()));
                        player.sendMessage(formatMessage("&7&l[&e&lGsign&7&l] &a&lMCID: &f" + creationData.getOwnerName()));
                    } else {
                        player.sendMessage(formatMessage("&7[&c!&7] &c&lいいね看板の作成に失敗しました。"));
                    }
                });

                signCreationMap.remove(playerId);
                break;
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
        Player player = event.getPlayer();

        if (block.getState() instanceof Sign) {
            if (signManager.isGoodSign(block.getLocation())) {
                if (!signManager.canBreakSign(block.getLocation(), player)) {
                    player.sendMessage(formatMessage("&7[&c!&7] &c&lこのいいね看板を破壊する権限がありません！"));
                    event.setCancelled(true);
                    return;
                }

                signManager.removeGoodSign(block.getLocation());
                player.sendMessage(formatMessage("&7[&b!&7] &a&lいいね看板を削除しました。"));
                return;
            }
        }

        Location upperLocation = block.getLocation().clone().add(0, 1, 0);
        Block upperBlock = upperLocation.getBlock();

        if (upperBlock.getState() instanceof Sign) {
            if (signManager.isGoodSign(upperLocation)) {
                if (!signManager.canBreakSign(upperLocation, player)) {
                    player.sendMessage(formatMessage("&7[&c!&7] &c&lこの看板は保護されているため、下のブロックを破壊できません！"));
                    event.setCancelled(true);
                    return;
                }
            }
        }

        checkWallSignProtection(block, player, event);
    }

    private void checkWallSignProtection(Block block, Player player, BlockBreakEvent event) {
        Location blockLoc = block.getLocation();

        Location[] directions = {
                blockLoc.clone().add(1, 0, 0),  // 東
                blockLoc.clone().add(-1, 0, 0), // 西
                blockLoc.clone().add(0, 0, 1),  // 南
                blockLoc.clone().add(0, 0, -1)  // 北
        };

        for (Location loc : directions) {
            Block adjacentBlock = loc.getBlock();
            if (adjacentBlock.getState() instanceof Sign) {
                Material signType = adjacentBlock.getType();
                if (isWallSign(signType)) {
                    if (signManager.isGoodSign(adjacentBlock.getLocation())) {
                        if (!signManager.canBreakSign(adjacentBlock.getLocation(), player)) {
                            player.sendMessage(formatMessage("&7[&c!&7] &c&lこの看板は保護されているため、このブロックを破壊できません！"));
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean isWallSign(Material material) {
        return material.name().contains("_WALL_SIGN");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        if (signManager.isGoodSign(block.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(org.bukkit.event.block.BlockFromToEvent event) {
        Block toBlock = event.getToBlock();
        if (toBlock.getState() instanceof Sign && signManager.isGoodSign(toBlock.getLocation())) {
            event.setCancelled(true);
        }
    }

    private String formatMessage(String message) {
        return message.replace("&", "§");
    }

    private static class SignCreationData {
        private final Location signLocation;
        private SignCreationStep step;
        private String title;
        private String ownerName;

        public SignCreationData(Location signLocation, SignCreationStep step) {
            this.signLocation = signLocation;
            this.step = step;
        }

        public Location getSignLocation() {
            return signLocation;
        }

        public SignCreationStep getStep() {
            return step;
        }

        public void setStep(SignCreationStep step) {
            this.step = step;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }
    }

    private enum SignCreationStep {
        TITLE,
        OWNER
    }
}