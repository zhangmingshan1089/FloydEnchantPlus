package cloud.floydzero.floydenchantplus.task;

import cloud.floydzero.floydenchantplus.FloydEnchantPlusPlugin;
import cloud.floydzero.floydenchantplus.config.PluginConfig;
import cloud.floydzero.floydenchantplus.util.HostileMobs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 雷霆审判（REQ-LOY-04）：
 * 以触发点为球心，对范围内全部敌方生物周期性（默认 1.5 秒/次）降下闪电，
 * 直到范围内不存在任何敌方生物；达到最大持续时长（默认 20 秒）时强制熔断并记录 Warn。
 *
 * <p>性能说明（PRD 6.1）：扫描频率严格为 strike_interval_ticks（严禁每 tick 扫描），
 * 单次为 30 格球形范围的实体枚举 + 距离过滤，主线程开销可忽略；
 * 召唤闪电本身必须在主线程执行，故未引入异步实体访问（Bukkit API 非线程安全）。</p>
 */
public final class ThunderJudgmentTask extends BukkitRunnable {

    private final FloydEnchantPlusPlugin plugin;
    private final Location center;
    private final UUID holderId;
    private final String holderName;

    private final Set<UUID> struck = new HashSet<>();
    private int killed = 0;
    private int elapsedTicks = 0;
    private boolean finished = false;

    public ThunderJudgmentTask(FloydEnchantPlusPlugin plugin, Location center, Player holder) {
        this.plugin = plugin;
        this.center = center.clone();
        this.holderId = holder.getUniqueId();
        this.holderName = holder.getName();
    }

    /** 启动任务：首轮打击立即执行，其后按 strike_interval_ticks 循环。 */
    public void start() {
        plugin.trackJudgment(this);
        runTaskTimer(plugin, 1L, plugin.pluginConfig().strikeIntervalTicks());
    }

    @Override
    public void run() {
        if (finished) {
            return;
        }
        PluginConfig cfg = plugin.pluginConfig();
        if (!center.isWorldLoaded()) {
            finish();
            return;
        }
        World world = center.getWorld();

        elapsedTicks += cfg.strikeIntervalTicks();

        // 结算上一轮被雷击生物的死亡情况（用于 killed 计数，PRD 6.3）
        Iterator<UUID> it = struck.iterator();
        while (it.hasNext()) {
            Entity entity = Bukkit.getEntity(it.next());
            if (!(entity instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                killed++;
                it.remove();
            }
        }

        // 目标重扫描（REQ-LOY-04 第 3 步）
        List<LivingEntity> targets = HostileMobs.collectHostiles(world, center, cfg.strikeRadius());
        if (targets.isEmpty()) {
            // 终结条件：范围内不存在任何敌方生物（REQ-LOY-04 第 4 步）
            finish();
            return;
        }

        Player holder = Bukkit.getPlayer(holderId);
        for (LivingEntity target : targets) {
            strike(world, target, holder, cfg);
            struck.add(target.getUniqueId());
        }

        // 强制熔断（REQ-LOY-04 第 5 步）
        if (elapsedTicks >= cfg.strikeMaxDurationSeconds() * 20L) {
            plugin.getLogger().warning("雷霆审判达到最大持续时长（" + cfg.strikeMaxDurationSeconds()
                    + " 秒）仍有敌方生物存活，已强制终止循环以防卡服。");
            finish();
        }
    }

    private void strike(World world, LivingEntity target, Player holder, PluginConfig cfg) {
        Location loc = target.getLocation();
        if (cfg.lightningFireDamage()) {
            // 服主显式允许火焰：使用真实闪电（可能点燃地形）
            world.strikeLightning(loc);
            return;
        }
        // 默认路径（lightning_fire_damage: false）：视觉闪电 + 主线程手动结算伤害，
        // 不产生火焰、不破坏地形（REQ-LOY-04 第 2 步），伤害归属持有者以便掉落/经验结算
        world.strikeLightningEffect(loc);
        if (holder != null && holder.isOnline() && holder.getWorld().equals(target.getWorld())) {
            target.damage(cfg.lightningDamage(), holder);
        } else {
            target.damage(cfg.lightningDamage());
        }
    }

    /** 结束任务并输出 PRD 6.3 规定格式的日志。 */
    public void finish() {
        if (finished) {
            return;
        }
        finished = true;
        plugin.untrackJudgment(this);
        cancel();
        String worldName = center.isWorldLoaded() ? center.getWorld().getName() : "unknown";
        plugin.getLogger().info("Player " + holderName + " triggered Loyalty IV at "
                + worldName + " [" + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ()
                + "], killed " + killed + " mobs.");
    }

    /** 供插件卸载时无条件终止（不重复输出日志）。 */
    public void abort() {
        if (finished) {
            return;
        }
        finished = true;
        plugin.untrackJudgment(this);
        cancel();
    }
}
