package cloud.floydzero.floydenchantplus.enchant;

import cloud.floydzero.floydenchantplus.FloydEnchantPlusPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * 铁砧合成升级（PRD 5.1 互斥设计）：
 * 当铁砧合并结果会使忠诚超过原版上限 III（如 忠诚 III 三叉戟 + 忠诚 III 附魔书），
 * 产物升级为<b>忠诚 IV</b>：原版忠诚归位 III + PDC 标记 + Lore 展示，
 * 不与原版忠诚 I-III 重复显示。
 */
public final class AnvilMergeListener implements Listener {

    private final FloydEnchantPlusPlugin plugin;

    public AnvilMergeListener(FloydEnchantPlusPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);
        if (first == null || second == null) {
            return;
        }

        ItemStack base;
        ItemStack other;
        if (first.getType() == Material.TRIDENT) {
            base = first;
            other = second;
        } else if (second.getType() == Material.TRIDENT) {
            base = second;
            other = first;
        } else {
            return; // 附魔仅适用于三叉戟（PRD 1.2）
        }

        LoyaltyIV loyalty = plugin.loyaltyIV();
        if (loyalty.has(base)) {
            return; // 已是忠诚 IV，交还原版逻辑
        }
        int baseLevel = loyalty.vanillaLoyaltyLevel(base);
        int otherLevel = loyalty.vanillaLoyaltyLevel(other);
        if (baseLevel <= 0 || otherLevel <= 0) {
            return;
        }
        if (baseLevel + otherLevel <= Enchantment.LOYALTY.getMaxLevel()) {
            return; // 原版上限内，交给原版合并
        }

        // 忠诚叠加将超过 III → 升级为忠诚 IV
        event.setResult(loyalty.apply(base));
    }
}
