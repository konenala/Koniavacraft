# 🎮 RPG 系統框架

這是 Koniavacraft 模組的 RPG 系統核心框架。

## 📁 目錄結構

```
rpg/
├── RPGManager.java              # RPG 系統管理器 (核心入口)
├── attributes/
│   └── PlayerAttributes.java   # 五大屬性系統
├── player/
│   └── PlayerClass.java         # 職業枚舉 (戰士/法師/遊俠)
├── data/
│   └── PlayerRPGData.java       # 玩家 RPG 數據 (等級/經驗/屬性)
└── skill/
    ├── Skill.java               # 技能抽象基類
    ├── SkillType.java           # 技能類型枚舉
    ├── SkillRegistry.java       # 技能註冊表
    └── PlayerSkillData.java     # 玩家技能數據
```

## 🎯 核心系統

### 1. 五大屬性系統

| 屬性 | 英文 | 效果 |
|------|------|------|
| 力量 | Strength | 每點 +2% 近戰傷害 |
| 智力 | Intelligence | 每點 +2% 魔法傷害, +10 魔力 |
| 敏捷 | Agility | 每點 +1% 攻速, +0.5% 閃避 |
| 體質 | Vitality | 每點 +2 生命, +0.5% 減傷 |
| 感知 | Perception | 每點 -0.25% 技能冷卻 (上限 200 點 = 50%) |

### 2. 職業系統

- **戰士 (Warrior)**: 近戰物理輸出
- **法師 (Mage)**: 遠程魔法輸出
- **遊俠 (Ranger)**: 遠程物理輸出

### 3. 等級系統

- 最大等級: 100
- 每級獲得: 3 屬性點
- 經驗公式: `100 * (level^1.5)`

### 4. 技能系統

**技能類型**:
- MELEE: 近戰技能
- RANGED: 遠程技能
- MAGIC: 魔法技能
- BUFF: 增益技能
- DEBUFF: 減益技能
- UTILITY: 功能技能

**冷卻機制**:
- 基礎冷卻時間由技能決定
- 感知屬性可減少冷卻時間
- 冷卻縮減公式: `實際冷卻 = 基礎冷卻 * (1 - CDR)`

## 🚀 使用方法

### 訪問玩家數據

```java
// 獲取玩家 RPG 數據
PlayerRPGData data = RPGManager.getPlayerData(player);

// 獲取等級
int level = data.getLevel();

// 獲取屬性
PlayerAttributes attrs = data.getAttributes();
int strength = attrs.getStrength();
```

### 給予經驗值

```java
// 給予 100 經驗
boolean leveledUp = RPGManager.giveExperience(player, 100);
if (leveledUp) {
    // 玩家升級了!
}
```

### 分配屬性點

```java
// 分配 5 點到力量
boolean success = RPGManager.allocateAttribute(player, "strength", 5);
```

### 計算傷害

```java
// 計算近戰傷害 (考慮力量加成)
float actualDamage = RPGManager.calculateMeleeDamage(player, 10.0f);

// 計算魔法傷害 (考慮智力加成)
float magicDamage = RPGManager.calculateMagicDamage(player, 15.0f);
```

### 技能冷卻

```java
// 計算技能實際冷卻時間 (考慮感知屬性)
int baseCooldown = 200; // 10 秒
int actualCooldown = RPGManager.calculateSkillCooldown(player, baseCooldown);
```

## 📝 待實現功能

目前這是**框架階段**,以下功能需要後續實現:

### 核心功能
- [ ] NeoForge Attachment 數據存儲
- [ ] 客戶端-伺服端數據同步
- [ ] 玩家死亡時數據保留

### 技能系統
- [ ] 具體技能實現 (火球術、重擊等)
- [ ] 技能傷害計算
- [ ] 技能粒子效果
- [ ] 技能音效

### UI 系統
- [ ] 屬性面板 UI
- [ ] 技能欄 UI
- [ ] 經驗條顯示
- [ ] 等級顯示

### 事件系統
- [ ] 升級事件 (音效/粒子/訊息)
- [ ] 傷害計算事件整合
- [ ] 經驗獲取事件

### 網路封包
- [ ] 經驗同步封包
- [ ] 屬性同步封包
- [ ] 技能施放封包

## 🔧 整合到模組

在 `KoniavacraftMod.java` 中初始化:

```java
public KoniavacraftMod(IEventBus modEventBus, ModContainer modContainer) {
    // ... 現有代碼 ...

    // 初始化 RPG 系統
    RPGManager.init();
}
```

## 📚 擴展指南

### 添加新技能

1. 創建技能類繼承 `Skill`:
```java
public class FireballSkill extends Skill {
    public FireballSkill() {
        super("fireball", "火球術", 200, 50, SkillType.MAGIC);
    }

    @Override
    public boolean cast(Player player, Level level) {
        // 實現技能邏輯
        return true;
    }

    @Override
    public String getDescription() {
        return "發射一個火球攻擊敵人";
    }
}
```

2. 在 `SkillRegistry.init()` 中註冊:
```java
register(new FireballSkill());
```

### 添加新屬性

如果需要第六個屬性,在 `PlayerAttributes` 中:

1. 添加屬性欄位
2. 添加 getter/setter
3. 添加計算方法
4. 更新 NBT 序列化
5. 更新 `getTotalAttributePoints()`

## ⚠️ 注意事項

1. **所有枚舉不使用中文** - 枚舉 ID 必須使用英文
2. **顯示名稱使用中文** - displayName 可以使用中文
3. **線程安全** - 數據訪問需要考慮客戶端/伺服端
4. **數據同步** - 修改數據後要同步到客戶端

## 📞 聯絡

如有問題或建議,請參考 `CLAUDE.md` 文件。
