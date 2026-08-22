package cloud.floydzero.floydenchantplus.enchant;

import cloud.floydzero.floydenchantplus.FloydEnchantPlusPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 忠诚 IV（Loyalty IV）附魔模型。
 *
 * <p>实现方式（与 PRD 3.1 / 5.1 对齐）：</p>
 * <ul>
 *   <li>物品内含<b>原版忠诚 III</b> —— 完全复用原版投掷回收物理，满足 REQ-LOY-01
 *       「与原版忠诚 III 的返回逻辑完全兼容」；</li>
 *   <li>PDC 标记 {@code floydenchantplus:loyalty_iv} —— 本插件识别附魔的唯一依据；</li>
 *   <li>灰色 Lore 行「§7忠诚 IV」—— 提示框展示（PRD 5.1），等级语义为 4；</li>
 *   <li>可选隐藏原版附魔行（{@code hide_vanilla_enchant_lore}），避免「忠诚 III」重复显示。</li>
 * </ul>
 */
public final class LoyaltyIV {

    /** 提示框中的附魔名（PRD 5.1：§7 灰色「忠诚 IV」）。 */
    public static final String LORE_LINE = "§7忠诚 IV";
    /** 附魔等级语义为 4（忠诚 IV）。 */
    public static final int LEVEL = 4;
    /** 底层承载的原版忠诚等级（驱动投掷回收）。 */
    public static final int VANILLA_LOYALTY_LEVEL = 3;

    private final FloydEnchantPlusPlugin plugin;
    private final NamespacedKey markerKey;

    public LoyaltyIV(FloydEnchantPlusPlugin plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "loyalty_iv");
    }

    /** 判断物品是否带有忠诚 IV。 */
    public boolean has(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    /** 创建一把全新的忠诚 IV 三叉戟。 */
    public ItemStack createTrident() {
        return apply(new ItemStack(Material.TRIDENT));
    }

    /**
     * 为物品附加忠诚 IV（返回新副本）：
     * 原版忠诚统一归位到 III（PRD 5.1「覆盖原版忠诚，避免重复显示」）+ PDC 标记 + Lore 行。
     */
    public ItemStack apply(ItemStack base) {
        ItemStack item = base.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        meta.removeEnchant(Enchantment.LOYALTY);
        meta.addEnchant(Enchantment.LOYALTY, VANILLA_LOYALTY_LEVEL, true);
        if (plugin.pluginConfig().hideVanillaEnchantLore()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.contains(LORE_LINE)) {
            lore.add(LORE_LINE);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 持有判定（REQ-LOY-02）：主手 → 副手 → 盔甲四槽位，任一槽位含忠诚 IV 即满足。
     * 背包普通格子不计入（TC-03）。
     */
    public ItemStack findEquipped(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] candidates = {
                inv.getItemInMainHand(),
                inv.getItemInOffHand(),
                inv.getHelmet(),
                inv.getChestplate(),
                inv.getLeggings(),
                inv.getBoots()
        };
        for (ItemStack item : candidates) {
            if (has(item)) {
                return item;
            }
        }
        return null;
    }

    /** 读取物品的原版忠诚等级（无则 0）。 */
    public int vanillaLoyaltyLevel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        return meta == null ? 0 : meta.getEnchantLevel(Enchantment.LOYALTY);
    }
}
