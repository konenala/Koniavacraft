# 原魔塵的低效不穩定設計

## 🤔 **概念解釋：為什麼原魔塵"可用但低效"？**

在您的世界觀中，原魔塵就像是"未精煉的石油"：
- 可以直接燃燒，但效率很低
- 容易產生雜質和副產品
- 精煉後才能發揮真正的潛力

## 💡 **設計理念：漸進式效率提升**

### 🔥 **原魔塵的直接使用**
```java
// 魔力生產機燃料配方
ManaGenFuelRecipeBuilder.create(ModItems.RAW_MANA_DUST.get(), 
    5,    // 魔力輸出：很少
    10,   // 能量輸出：很少  
    600,  // 燃燒時間：很慢
    0.3f  // 成功率：30%失敗率
).save(output);

// 對比：魔力粉的效率
ManaGenFuelRecipeBuilder.create(ModItems.MANA_DUST.get(), 
    50,   // 魔力輸出：高10倍
    25,   // 能量輸出：高2.5倍
    800,  // 燃燒時間：更快
    1.0f  // 成功率：100%穩定
).save(output);
```

### ⚗️ **不穩定的副產品機制**
```java
// 原魔塵熔爐配方（帶副產品）
SimpleCookingRecipeBuilder.smelting(
    Ingredient.of(ModItems.RAW_MANA_DUST.get()),
    RecipeCategory.MISC,
    ModItems.MANA_DUST.get(),
    0.1f,  // 低經驗
    300    // 較慢的熔煉時間
)
.withByproduct(ModItems.CORRUPTED_MANA_DUST.get(), 0.15f) // 15%機率產生汙穢魔力粉
.save(output);
```

### 🔧 **早期臨時解決方案**
```java
// 緊急情況的低效合成
ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_DUST.get())
    .requires(ModItems.RAW_MANA_DUST.get(), 3) // 3個原魔塵
    .requires(Items.COAL)                       // + 煤炭
    .unlockedBy("has_raw_mana", has(ModItems.RAW_MANA_DUST.get()))
    .save(output, "emergency_mana_dust_from_raw");
```

## 🎯 **玩家體驗設計**

### 📈 **效率對比表**
| 方法 | 投入 | 產出 | 效率 | 副產品 |
|------|------|------|------|--------|
| 直接使用原魔塵 | 1個 | 5魔力 | 極低 | 可能爆炸 |
| 緊急合成 | 3個+煤炭 | 1魔力粉 | 低 | 無 |
| 熔爐精煉 | 1個 | 1魔力粉 | 正常 | 15%汙穢魔力粉 |
| 高爐精煉 | 1個 | 1魔力粉 | 高 | 25%汙穢魔力粉 |

### 🔄 **漸進式改善**
1. **第1天**：直接燒原魔塵，效率極低但能用
2. **第3天**：學會緊急合成，稍微好一點
3. **第5天**：建造熔爐，獲得穩定產出
4. **第10天**：升級到高爐，效率最佳

## 🎮 **世界觀故事化**
- **原魔塵**："這些未處理的魔力結晶不太穩定，直接使用可能會爆炸..."
- **緊急合成**："用煤炭混合可以勉強穩定魔力，但損失很大。"
- **熔爐精煉**："高溫處理讓魔力更純淨，偶爾還能提取出汙穢的雜質。"
- **高爐精煉**："更高的溫度和壓力，讓精煉過程更有效率！"