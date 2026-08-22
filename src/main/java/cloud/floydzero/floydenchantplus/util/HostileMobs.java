package cloud.floydzero.floydenchantplus.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Tameable;

import java.util.ArrayList;
import java.util.List;

/**
 * 敌方生物（Hostile Mob）筛选（PRD 1.3 术语定义 / REQ-LOY-04 目标筛选）。
 *
 * <p>包含：僵尸、骷髅、苦力怕、蜘蛛、卫道士、掠夺者、尸壳、流浪者、烈焰人、
 * 猪灵蛮兵等一切敌对生物（含恶魂、岩浆怪、潜影贝、幻翼等非 Monster 继承树的主动攻击型生物）。</p>
 *
 * <p>排除：玩家、被动动物、未激怒的中立生物（末影人仅在激怒/索敌时纳入）、
 * 铁傀儡、雪傀儡等友好构造体，以及已驯服宠物。</p>
 */
public final class HostileMobs {

    private HostileMobs() {
    }

    /**
     * 收集以 center 为球心、radius 为半径的球形范围内的全部存活敌方生物。
     * （先取立方包围盒再过滤球面距离，保证 31 格外的怪物不受影响 —— TC-07。）
     */
    public static List<LivingEntity> collectHostiles(World world, Location center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        double radiusSq = radius * radius;
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!living.isValid() || living.isDead()) {
                continue;
            }
            if (living.getLocation().distanceSquared(center) > radiusSq) {
                continue;
            }
            if (isHostile(living)) {
                result.add(living);
            }
        }
        return result;
    }

    private static boolean isHostile(LivingEntity living) {
        if (living instanceof Player) {
            return false; // 不包含玩家（PRD 1.3）
        }
        if (!(living instanceof Mob mob)) {
            return false; // 无 AI 实体一律排除
        }
        if (living instanceof AbstractVillager || living instanceof IronGolem || living instanceof Snowman) {
            return false; // 村民/流浪商人、铁傀儡、雪傀儡（PRD 3.4 排除对象）
        }
        if (living instanceof Tameable tameable && tameable.isTamed()) {
            return false; // 已驯服宠物
        }
        // 中立生物：末影人仅在激怒（正在索敌）时包含（PRD 3.4）
        if (living instanceof Enderman) {
            return mob.getTarget() != null;
        }
        // 僵尸/骷髅/苦力怕/蜘蛛/卫道士/掠夺者/尸壳/流浪者/烈焰人/猪灵蛮兵等
        if (living instanceof Monster) {
            return true;
        }
        // 不在 Monster 继承树内、但会主动攻击玩家的敌对生物
        return switch (living.getType()) {
            case GHAST, SLIME, MAGMA_CUBE, PHANTOM, SHULKER, HOGLIN, ZOGLIN, BREEZE, WARDEN -> true;
            default -> false;
        };
    }
}
