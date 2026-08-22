package cloud.floydzero.floydenchantplus.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 物品耐久工具（REQ-LOY-05）。
 */
public final class Durability {

    private Durability() {
    }

    /**
     * 强制消耗耐久度（无视耐久/经验修补减免，触发瞬间优先结算 —— PRD 6.2）。
     *
     * @return true 表示物品耐久不足、已直接损毁消失（TC-06）
     */
    public static boolean consume(ItemStack item, int amount) {
        if (item == null || item.getType() == Material.AIR || amount <= 0) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.isUnbreakable()) {
            return false; // 不可破坏物品不扣耐久
        }
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }
        int max = item.getType().getMaxDurability();
        if (max <= 0) {
            return false;
        }
        int newDamage = damageable.getDamage() + amount;
        if (newDamage >= max) {
            item.setAmount(0);
            return true;
        }
        damageable.setDamage(newDamage);
        item.setItemMeta(damageable);
        return false;
    }
}
