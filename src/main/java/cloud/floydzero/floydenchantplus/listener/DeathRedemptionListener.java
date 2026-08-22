package cloud.floydzero.floydenchantplus.listener;

import cloud.floydzero.floydenchantplus.FloydEnchantPlusPlugin;
import cloud.floydzero.floydenchantplus.config.PluginConfig;
import cloud.floydzero.floydenchantplus.task.ThunderJudgmentTask;
import cloud.floydzero.floydenchantplus.util.Durability;
import cloud.floydzero.floydenchantplus.util.Messages;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 死亡救赎监听器（REQ-LOY-02 / REQ-LOY-03 / REQ-LOY-05）。
 *
 * <p>在致命伤害落地前拦截（{@link EventPriority#HIGH}，晚于多数插件的伤害修改、
 * 早于 MONITOR 记录层）：非虚空伤害 + 持有忠诚 IV + 不在冷却 →
 * 取消本次伤害，执行不死图腾救赎，并立即开启雷霆审判。</p>
 *
 * <p>由于伤害事件在致死前被取消，PlayerDeathEvent 永远不会触发，
 * 因此天然不干涉其他插件对死亡事件的监听（PRD 6.2）。</p>
 */
public final class DeathRedemptionListener implements Listener {

    /** 双重触发保护窗口（毫秒）：窗口内同一玩家的第二次致命判定直接忽略（PRD 6.2）。 */
    private static final long SAVE_GUARD_MS = 1000L;

    /** 生命恢复 II，45 秒（REQ-LOY-03）。 */
    private static final int REGEN_DURATION_TICKS = 900;
    private static final int REGEN_AMPLIFIER = 1;
    /** 伤害吸收 IV，5 秒（REQ-LOY-03，效果等级参考不死图腾体系）。 */
    private static final int ABSORPTION_DURATION_TICKS = 100;
    private static final int ABSORPTION_AMPLIFIER = 3;

    private final FloydEnchantPlusPlugin plugin;
    private final Map<UUID, Long> recentSaves = new HashMap<>();

    public DeathRedemptionListener(FloydEnchantPlusPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        PluginConfig cfg = plugin.pluginConfig();
        if (!cfg.enabled()) {
            return;
        }
        // 虚空伤害硬性排除（REQ-LOY-02，v1.1 更新；TC-01）
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        double health = player.getHealth();
        if (health <= 0.0D) {
            return;
        }
        if (event.getFinalDamage() < health) {
            return; // 非致命伤害，不处理
        }

        long now = System.currentTimeMillis();
        Long lastSave = recentSaves.get(player.getUniqueId());
        if (lastSave != null && now - lastSave < SAVE_GUARD_MS) {
            return; // 双重触发保护（PRD 6.2）
        }

        // 冷却判定（REQ-LOY-05；TC-05）：冷却中提示并正常死亡
        if (plugin.cooldownManager().isOnCooldown(player.getUniqueId())) {
            Messages.sendActionBar(player, Messages.ACTIONBAR_COOLDOWN);
            return;
        }

        // 持有判定（REQ-LOY-02）：主手/副手/盔甲任一槽位（TC-03）
        ItemStack source = plugin.loyaltyIV().findEquipped(player);
        if (source == null) {
            return;
        }

        event.setCancelled(true);
        recentSaves.put(player.getUniqueId(), now);
        redeem(player, source, cfg);
    }

    /** 不死图腾救赎（REQ-LOY-03）+ 耐久/冷却结算（REQ-LOY-05）+ 雷霆审判（REQ-LOY-04）。 */
    private void redeem(Player player, ItemStack source, PluginConfig cfg) {
        // —— 救赎：取消死亡后强制 1.0 血（半颗心），熄灭着火，施加图腾级状态 ——
        player.setHealth(1.0D);
        player.setFireTicks(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                REGEN_DURATION_TICKS, REGEN_AMPLIFIER, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
                ABSORPTION_DURATION_TICKS, ABSORPTION_AMPLIFIER, false, true, true));

        // —— 表现反馈：图腾音效 + 图腾粒子（PRD 3.3 / 5.2） ——
        if (cfg.playTotemAnimation()) {
            Location loc = player.getLocation();
            player.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.0F, 1.0F);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    loc.clone().add(0, 1.0, 0), 40, 0.5, 0.8, 0.5, 0.15);
        }

        // —— 物品不消耗，但强制扣 150 耐久；不足则损毁，救赎与雷罚照常（TC-06） ——
        boolean destroyed = Durability.consume(source, cfg.durabilityCost());
        if (destroyed) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
        }

        // —— 全局冷却：按 UUID 记录，跨世界不重置，持久化到 cooldowns.yml ——
        plugin.cooldownManager().set(player.getUniqueId(), cfg.cooldownSeconds());

        // —— 持有者提示 + 可选全服广播（PRD 5.2） ——
        Messages.sendActionBar(player, Messages.ACTIONBAR_TRIGGER);
        if (cfg.broadcastTrigger()) {
            Messages.broadcast(Messages.triggerBroadcast(player.getName()));
        }

        // —— 雷霆审判：救赎成功后立即执行（REQ-LOY-04） ——
        new ThunderJudgmentTask(plugin, player.getLocation(), player).start();
    }
}
