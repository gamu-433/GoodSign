package me.petoma21.goodSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GoodSignCommand implements CommandExecutor {

    private final GoodSignManager signManager;
    private final ConfigManager configManager;
    private final RankingGUI rankingGUI;

    public GoodSignCommand(GoodSignManager signManager, ConfigManager configManager, RankingGUI rankingGUI) {
        this.signManager = signManager;
        this.configManager = configManager;
        this.rankingGUI = rankingGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(formatMessage("&a=== GoodSign コマンド ==="));
            sender.sendMessage(formatMessage("&e/gsign list <ユーザー名> &f- ユーザーのいいね看板一覧"));
            sender.sendMessage(formatMessage("&e/gsign gui &f- いいねランキングGUI"));
            sender.sendMessage(formatMessage("&e/gsign reload &f- 設定ファイルの再読み込み"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleListCommand(sender, args);

            case "gui":
                return handleGUICommand(sender);

            case "reload":
                return handleReloadCommand(sender);

            default:
                sender.sendMessage(formatMessage("&c不明なコマンドです。/gsign でヘルプを表示します。"));
                return true;
        }
    }

    private boolean handleListCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(formatMessage("&c使用方法: /gsign list <ユーザー名>"));
            return true;
        }

        if (!sender.hasPermission("goodsign.list")) {
            sender.sendMessage(formatMessage("&cこのコマンドを使用する権限がありません！"));
            return true;
        }

        String targetUser = args[1];
        List<GoodSignData> userSigns = signManager.getUserSigns(targetUser);

        if (userSigns.isEmpty()) {
            sender.sendMessage(formatMessage("&c" + targetUser + " のいいね看板は見つかりませんでした。"));
            return true;
        }

        sender.sendMessage(formatMessage("&a=== " + targetUser + " のいいね看板一覧 ==="));
        for (int i = 0; i < userSigns.size(); i++) {
            GoodSignData signData = userSigns.get(i);
            sender.sendMessage(formatMessage("&e" + (i + 1) + ". &f" + signData.getTitle()));
            sender.sendMessage(formatMessage("   &7場所: " + signData.getLocationString()));
            sender.sendMessage(formatMessage("   &7いいね数: &c" + signData.getLikes()));
            sender.sendMessage("");
        }
        sender.sendMessage(formatMessage("&a合計: " + userSigns.size() + "個"));

        return true;
    }

    private boolean handleGUICommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatMessage("&cこのコマンドはプレイヤーのみ実行できます！"));
            return true;
        }

        if (!sender.hasPermission("goodsign.gui")) {
            sender.sendMessage(formatMessage("&cこのコマンドを使用する権限がありません！"));
            return true;
        }

        Player player = (Player) sender;
        rankingGUI.openRankingGUI(player);
        player.sendMessage(formatMessage("&aいいねランキングを開きました！"));

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("goodsign.reload")) {
            sender.sendMessage(formatMessage("&cこのコマンドを使用する権限がありません！"));
            return true;
        }

        configManager.reload();
        sender.sendMessage(formatMessage("&a設定ファイルを再読み込みしました！"));

        return true;
    }

    private String formatMessage(String message) {
        return message.replace("&", "§");
    }
}