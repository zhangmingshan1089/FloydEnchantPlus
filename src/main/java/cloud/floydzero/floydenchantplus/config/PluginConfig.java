package cloud.floydzero.floydenchantplus.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * {@code enchants.loyal_iv} 配置节点的类型化封装（对应 PRD 第 4 节）。
 * 所有字段均带默认值，配置缺失时回退到 PRD 推荐值。
 */
public final class PluginConfig {

    private boolean enabled;
    private double strikeRadius;
    private int strikeIntervalTicks;
    private int strikeMaxDurationSeconds;
    private int cooldownSeconds;
    private int durabilityCost;
    private boolean playTotemAnimation;
    private boolean lightningFireDamage;
    private double lightningDamage;
    private boolean broadcastTrigger;
    private boolean hideVanillaEnchantLore;

    public void load(FileConfiguration cfg) {
        this.enabled = cfg.getBoolean("enchants.loyal_iv.enabled", true);
        this.strikeRadius = cfg.getDouble("enchants.loyal_iv.strike_radius", 30.0D);
        this.strikeIntervalTicks = Math.max(1, cfg.getInt("enchants.loyal_iv.strike_interval_ticks", 30));
        this.strikeMaxDurationSeconds = Math.max(1, cfg.getInt("enchants.loyal_iv.strike_max_duration_seconds", 20));
        this.cooldownSeconds = Math.max(0, cfg.getInt("enchants.loyal_iv.cooldown_seconds", 300));
        this.durabilityCost = Math.max(0, cfg.getInt("enchants.loyal_iv.durability_cost", 150));
        this.playTotemAnimation = cfg.getBoolean("enchants.loyal_iv.play_totem_animation", true);
        this.lightningFireDamage = cfg.getBoolean("enchants.loyal_iv.lightning_fire_damage", false);
        this.lightningDamage = cfg.getDouble("enchants.loyal_iv.lightning_damage", 5.0D);
        this.broadcastTrigger = cfg.getBoolean("enchants.loyal_iv.broadcast_trigger", true);
        this.hideVanillaEnchantLore = cfg.getBoolean("enchants.loyal_iv.hide_vanilla_enchant_lore", true);
    }

    /** 是否启用该附魔。 */
    public boolean enabled() {
        return enabled;
    }

    /** 雷罚作用半径（格）。 */
    public double strikeRadius() {
        return strikeRadius;
    }

    /** 雷击间隔（刻）。 */
    public int strikeIntervalTicks() {
        return strikeIntervalTicks;
    }

    /** 雷罚最大持续时长（秒），强制熔断上限。 */
    public int strikeMaxDurationSeconds() {
        return strikeMaxDurationSeconds;
    }

    /** 触发后的冷却时间（秒）。 */
    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    /** 触发时消耗的耐久度。 */
    public int durabilityCost() {
        return durabilityCost;
    }

    /** 是否播放图腾动画（音效 + 粒子）。 */
    public boolean playTotemAnimation() {
        return playTotemAnimation;
    }

    /** 是否允许闪电破坏地形（true 时使用真实闪电）。 */
    public boolean lightningFireDamage() {
        return lightningFireDamage;
    }

    /** 手动结算路径下的单次雷击伤害。 */
    public double lightningDamage() {
        return lightningDamage;
    }

    /** 是否全服广播触发消息。 */
    public boolean broadcastTrigger() {
        return broadcastTrigger;
    }

    /** 是否隐藏原版附魔提示行（避免「忠诚 III」重复显示）。 */
    public boolean hideVanillaEnchantLore() {
        return hideVanillaEnchantLore;
    }
}
