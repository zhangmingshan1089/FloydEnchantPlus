package cloud.floydzero.floydenchantplus;

import cloud.floydzero.floydenchantplus.command.FepCommand;
import cloud.floydzero.floydenchantplus.config.PluginConfig;
import cloud.floydzero.floydenchantplus.enchant.AnvilMergeListener;
import cloud.floydzero.floydenchantplus.enchant.LoyaltyIV;
import cloud.floydzero.floydenchantplus.listener.DeathRedemptionListener;
import cloud.floydzero.floydenchantplus.manager.CooldownManager;
import cloud.floydzero.floydenchantplus.task.ThunderJudgmentTask;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * FloydEnchantPlus 主类。
 * 功能模块：忠诚 IV（Loyalty IV）附魔 —— 死亡救赎 + 雷霆审判。
 */
public final class FloydEnchantPlusPlugin extends JavaPlugin {

    private PluginConfig config;
    private LoyaltyIV loyaltyIV;
    private CooldownManager cooldownManager;
    private final List<ThunderJudgmentTask> activeJudgments = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = new PluginConfig();
        this.config.load(getConfig());
        this.loyaltyIV = new LoyaltyIV(this);
        this.cooldownManager = new CooldownManager(this);

        getServer().getPluginManager().registerEvents(new DeathRedemptionListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilMergeListener(this), this);

        PluginCommand command = getCommand("floydenchantplus");
        if (command != null) {
            FepCommand executor = new FepCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("FloydEnchantPlus 已启用 —— 忠诚 IV 附魔就绪。");
    }

    @Override
    public void onDisable() {
        for (ThunderJudgmentTask task : new ArrayList<>(activeJudgments)) {
            task.abort();
        }
        activeJudgments.clear();
        if (cooldownManager != null) {
            cooldownManager.save();
        }
        getLogger().info("FloydEnchantPlus 已卸载。");
    }

    /** 重载 config.yml（/fep reload）。 */
    public void reloadPluginConfig() {
        reloadConfig();
        this.config.load(getConfig());
    }

    public PluginConfig pluginConfig() {
        return config;
    }

    public LoyaltyIV loyaltyIV() {
        return loyaltyIV;
    }

    public CooldownManager cooldownManager() {
        return cooldownManager;
    }

    public void trackJudgment(ThunderJudgmentTask task) {
        activeJudgments.add(task);
    }

    public void untrackJudgment(ThunderJudgmentTask task) {
        activeJudgments.remove(task);
    }
}
