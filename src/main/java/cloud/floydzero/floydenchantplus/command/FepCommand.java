package cloud.floydzero.floydenchantplus.command;

import cloud.floydzero.floydenchantplus.FloydEnchantPlusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /fep 主命令：give（发放三叉戟）/ apply（附魔主手）/ reload（重载配置）。
 */
public final class FepCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN = "floydenchantplus.admin";

    private final FloydEnchantPlusPlugin plugin;

    public FepCommand(FloydEnchantPlusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> handleGive(sender, args);
            case "apply" -> handleApply(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage("§c你没有权限执行该命令。");
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§c玩家 §e" + args[1] + " §c不在线。");
                return;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage("§c用法: /fep give <玩家>");
            return;
        }

        ItemStack trident = plugin.loyaltyIV().createTrident();
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(trident);
        leftover.values().forEach(item ->
                target.getWorld().dropItemNaturally(target.getLocation(), item));
        sender.sendMessage("§a已将忠诚 IV 三叉戟给予 §e" + target.getName() + "§a。");
        target.sendMessage("§a你获得了 §7忠诚 IV §a三叉戟！");
    }

    private void handleApply(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行，请使用 /fep give <玩家>。");
            return;
        }
        if (!player.hasPermission(PERM_ADMIN)) {
            player.sendMessage("§c你没有权限执行该命令。");
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        // PRD 1.2：仅适用于三叉戟，强制附加于其他物品时警告并忽略
        if (hand.getType() != Material.TRIDENT) {
            player.sendMessage("§c[警告] 忠诚 IV 仅适用于三叉戟（TRIDENT），已忽略本次操作。");
            return;
        }
        if (plugin.loyaltyIV().has(hand)) {
            player.sendMessage("§7当前主手物品已是忠诚 IV。");
            return;
        }
        player.getInventory().setItemInMainHand(plugin.loyaltyIV().apply(hand));
        player.sendMessage("§a已为当前主手三叉戟附魔 §7忠诚 IV§a！");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage("§c你没有权限执行该命令。");
            return;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage("§aFloydEnchantPlus 配置已重载。");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§dFloydEnchantPlus §7- 忠诚 IV 附魔（死亡救赎 + 雷霆审判）");
        sender.sendMessage("§e/" + label + " give [玩家] §7- 给予忠诚 IV 三叉戟");
        sender.sendMessage("§e/" + label + " apply §7- 将当前主手三叉戟附魔为忠诚 IV");
        sender.sendMessage("§e/" + label + " reload §7- 重载配置文件");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("give", "apply", "reload"));
            subs.removeIf(sub -> !sub.startsWith(args[0].toLowerCase(Locale.ROOT)));
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
