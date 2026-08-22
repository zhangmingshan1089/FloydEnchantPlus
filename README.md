# FloydEnchantPlus

> 忠诚 IV（Loyalty IV）附魔 —— 死亡救赎 + 雷霆审判
> 基于 Paper/Spigot API，支持 Minecraft 1.21.x

依据《docs/忠诚四附魔机制PRD.md》（v1.1）实现的 Bukkit 插件。

## 机制概览

持有忠诚 IV 的玩家遭受**非虚空来源**的致命伤害时：

1. **死亡救赎**：取消本次致命伤害，生命值重置为 1.0（半颗心），施加
   生命恢复 II（45s）与伤害吸收 IV（5s），熄灭着火，播放图腾音效 + 粒子；
2. **雷霆审判**：立即以触发点为球心，对 30 格内全部敌方生物降下闪电，
   每 1.5 秒重扫一次，直至范围内无敌方生物；最长 20 秒强制熔断；
3. **平衡约束**：每次触发强制消耗 150 点耐久（不足则三叉戟损毁，但救赎照常），
   玩家进入 5 分钟全局冷却（跨世界、跨重启持久化于 `cooldowns.yml`）。

虚空伤害（`DamageCause.VOID`）**硬编码不触发**，玩家正常死亡。

## 构建

要求 JDK 17+ 与 Maven 3.8+：

```bash
mvn package
```

产物：`target/FloydEnchantPlus-1.0.0.jar`，放入服务器 `plugins/` 目录即可。

## 获取忠诚 IV 三叉戟

- 命令：`/fep give [玩家]`（OP，权限 `floydenchantplus.admin`）
- 命令：`/fep apply`（附魔当前主手三叉戟；非三叉戟会警告并忽略 —— PRD 1.2）
- 铁砧：忠诚 III 三叉戟 + 忠诚 III 附魔书（或其他使忠诚叠加超过 III 的组合）
  → 合并产物自动升级为忠诚 IV

## 实现说明

| PRD 需求 | 实现位置 |
| --- | --- |
| REQ-LOY-01 投掷回收（兼容原版忠诚 III） | 物品内含原版忠诚 III，回收物理完全由原版驱动；`enchant/LoyaltyIV.java` |
| REQ-LOY-02 触发条件（致命伤 / 持有判定 / 虚空排除 / 冷却判定） | `listener/DeathRedemptionListener.java` |
| REQ-LOY-03 不死图腾救赎 | `DeathRedemptionListener#redeem` |
| REQ-LOY-04 雷霆审判（目标筛选 / 周期雷击 / 熔断） | `task/ThunderJudgmentTask.java` + `util/HostileMobs.java` |
| REQ-LOY-05 耐久消耗与全局冷却 | `util/Durability.java` + `manager/CooldownManager.java` |
| 配置参数（PRD 第 4 节） | `src/main/resources/config.yml` |
| 附魔显示与铁砧互斥升级（PRD 5.1） | `enchant/LoyaltyIV.java` + `enchant/AnvilMergeListener.java` |
| 日志格式（PRD 6.3） | `ThunderJudgmentTask#finish` |

### 附魔的承载方式

忠诚 IV = **原版忠诚 III**（驱动投掷回收，REQ-LOY-01 的"完全兼容"）
+ **PDC 标记** `floydenchantplus:loyalty_iv`（插件识别依据）
+ **Lore 行「§7忠诚 IV」**（提示框展示）。

`hide_vanilla_enchant_lore: true`（默认）时隐藏原版附魔提示行，避免
「忠诚 III」与「忠诚 IV」重复显示（PRD 5.1 建议互斥）。注意这会一并隐藏
其他原版附魔行（如经验修补），功能不受影响；如需显示可改为 `false`。

### 敌方生物判定

`Monster` 继承树（僵尸/骷髅/苦力怕/蜘蛛/掠夺者/烈焰人/猪灵蛮兵等）
+ 非 Monster 但主动攻击玩家的生物（恶魂/史莱姆/岩浆怪/幻翼/潜影贝/浩鲸/旋风人/监守者等）。
排除：玩家、被动动物、未激怒的末影人、村民、铁傀儡、雪傀儡、已驯服宠物。

### 性能（PRD 6.1）

雷击扫描严格按 `strike_interval_ticks`（默认 30 刻 = 1.5 秒）执行，
单次为主线程 30 格球形实体枚举 + 距离过滤，开销可忽略；
召唤闪电必须主线程执行，故未采用异步实体访问（Bukkit API 非线程安全）。

### 与其他插件的兼容（PRD 6.2）

本插件在 `EntityDamageEvent`（HIGH 优先级）取消致命伤害，
`PlayerDeathEvent` 不会触发，因此不干涉其他插件的死亡监听；
内置 1 秒双重触发保护窗口，避免与其他救援类插件重复触发。

## 测试验收对照（PRD 第 7 节）

| 编号 | 场景 | 预期 | 对应逻辑 |
| --- | --- | --- | --- |
| TC-01 | 跳虚空（Y<-64） | 不触发，正常死亡 | `DamageCause.VOID` 硬排除 |
| TC-02 | 怪物攻击致死 | 复活 + 30 格雷罚 | 致命伤拦截 → 救赎 → 审判 |
| TC-03 | 三叉戟在背包深处 | 不触发 | 仅扫描主手/副手/盔甲槽 |
| TC-04 | 30 格内 5 只僵尸 | 每 1.5s 雷击直至清空 | 周期重扫 + 空目标终结 |
| TC-05 | 触发后 10s 再受致命伤 | 冷却提示，正常死亡 | `CooldownManager` + ActionBar |
| TC-06 | 耐久 100 触发 | 三叉戟损毁，救赎照常 | `Durability.consume` 先结算效果后扣耐久 |
| TC-07 | 怪物在 31 格外 | 不受影响 | 球形距离过滤 |
| TC-08 | `/kill` 死亡 | 正常触发（非 VOID） | 仅排除 `VOID` 类型 |
