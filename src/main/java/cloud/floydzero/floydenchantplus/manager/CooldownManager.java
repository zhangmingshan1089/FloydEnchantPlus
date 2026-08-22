package cloud.floydzero.floydenchantplus.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 全局冷却管理（REQ-LOY-05）。
 *
 * <p>按玩家 UUID 记录冷却到期时间戳：与物品、世界无关（跨世界不重置，
 * 冷却期间该玩家持有的任意忠诚 IV 物品均无法再次触发），并持久化到
 * {@code cooldowns.yml}，服务器重启后冷却依然有效。</p>
 */
public final class CooldownManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> expireAt = new HashMap<>();
    private final File file;

    public CooldownManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cooldowns.yml");
        load();
    }

    /** 玩家是否处于冷却中。 */
    public boolean isOnCooldown(UUID playerId) {
        return remainingSeconds(playerId) > 0L;
    }

    /** 剩余冷却秒数（向上取整，0 表示不在冷却）。 */
    public long remainingSeconds(UUID playerId) {
        Long until = expireAt.get(playerId);
        if (until == null) {
            return 0L;
        }
        long remainMs = until - System.currentTimeMillis();
        if (remainMs <= 0L) {
            expireAt.remove(playerId);
            return 0L;
        }
        return (long) Math.ceil(remainMs / 1000.0D);
    }

    /** 设置冷却（秒），并立即持久化。 */
    public void set(UUID playerId, int seconds) {
        if (seconds <= 0) {
            return;
        }
        expireAt.put(playerId, System.currentTimeMillis() + seconds * 1000L);
        save();
    }

    /** 保存到 cooldowns.yml。 */
    public synchronized void save() {
        purgeExpired();
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : expireAt.entrySet()) {
            yaml.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "保存冷却数据失败", ex);
        }
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                expireAt.put(UUID.fromString(key), yaml.getLong(key));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("冷却数据包含非法 UUID，已跳过: " + key);
            }
        }
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        expireAt.values().removeIf(until -> until <= now);
    }
}
