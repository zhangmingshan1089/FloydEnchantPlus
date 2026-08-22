package cloud.floydzero.floydenchantplus.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 文案与消息工具（PRD 第 5 节）。
 */
public final class Messages {

    /** 触发成功：持有者 ActionBar 提示（PRD 5.2）。 */
    public static final String ACTIONBAR_TRIGGER = "§dFloydEnchantPlus §f- 雷霆裁决已降临！";
    /** 冷却期间再次受到致命伤：ActionBar 提示（PRD 3.5）。 */
    public static final String ACTIONBAR_COOLDOWN = "§c忠诚 IV 正在冷却中...";

    private Messages() {
    }

    /** 发送支持 § 颜色代码的 ActionBar 消息。 */
    public static void sendActionBar(Player player, String legacyText) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacyText));
    }

    /** 向全服玩家与控制台广播。 */
    public static void broadcast(String legacyText) {
        Bukkit.getConsoleSender().sendMessage(legacyText);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(legacyText);
        }
    }

    /** 全服公告文案（PRD 5.2）：§e[!] §6[玩家名] §e触发了雷霆审判！ */
    public static String triggerBroadcast(String playerName) {
        return "§e[!] §6" + playerName + " §e触发了雷霆审判！";
    }
}
