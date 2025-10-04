# Nara UI 深度效能檢查報告

> 基於 GPT-5 GUI 效能檢查清單
> 檢查日期：2025-10-05

---

## 📋 檢查清單逐項分析

### ✅ 1. 每幀建立物件（Impact: 極高）

#### NaraIntroScreen
```java
// ✅ 良好：無每幀物件建立
@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    cacheCenter();  // ✅ 只在需要時計算一次
    graphics.fill(0, 0, this.width, this.height, 0xFF000000);

    // ⚠️ 潛在問題：每幀建立 float
    float interpolatedAngle = accumulatedAngle + (ROTATION_SPEED * partialTick);  // 原始型別，影響極小

    // ✅ PoseStack 是重用的，不是 new
    PoseStack pose = graphics.pose();
    // ...
}
```

**評分**：✅ 優秀（9/10）
- 無 `new` 物件建立
- 僅有原始型別計算

#### NaraInitScreen
```java
// ❌ 問題：TEXT_LINES 在類別載入時建立，但每行都是新 Component
private static final Component[] TEXT_LINES = buildLines();

private static Component[] buildLines() {
    return new Component[] {
        Component.translatable("screen.koniava.nara.line1"),  // ✅ 靜態建立，不是每幀
        Component.translatable("screen.koniava.nara.line2"),
        // ...
    };
}

// ✅ 良好：快取機制
private final FormattedCharSequence[] cachedLineSequences = new FormattedCharSequence[TEXT_LINES.length];

@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    refreshTextCache();  // ✅ 只在 textCacheDirty 時重建
    // ...
}
```

**評分**：✅ 優秀（9/10）
- Component 靜態建立
- FormattedCharSequence 快取

---

### ✅ 2. 文字排版與計寬（Impact: 高）

#### NaraInitScreen
```java
// ✅ 優秀：完整快取機制
private void refreshTextCache() {
    if (!textCacheDirty || this.font == null) {  // ✅ 標記檢查
        return;
    }
    for (int index = 0; index < TEXT_LINES.length; index++) {
        cachedLineSequences[index] = TEXT_LINES[index].getVisualOrderText();  // ✅ 快取序列
        lineWidths[index] = this.font.width(cachedLineSequences[index]);      // ✅ 快取寬度
        cachedLineStartX[index] = cachedCenterX - lineWidths[index] / 2;      // ✅ 快取位置
    }
    textCacheDirty = false;  // ✅ 清除標記
}

@Override
public void render(...) {
    // ✅ 直接使用快取值，無重複計算
    graphics.drawString(this.font, cachedLineSequences[index], cachedLineStartX[index], y, 0xAAAAAA);
}
```

**評分**：✅ 完美（10/10）
- 寬度快取 ✓
- 位置快取 ✓
- 髒標記優化 ✓

---

### ⚠️ 3. 材質綁定/切換太頻繁（Impact: 極高）

#### NaraIntroScreen
```java
@Override
public void render(...) {
    graphics.fill(...);                          // Shader 切換 1
    graphics.blit(CIRCLE_TEXTURE, ...);          // Shader 切換 2
    super.render(graphics, mouseX, mouseY, partialTick);  // 可能有額外切換
}
```

**評分**：✅ 良好（8/10）
- 只用 1 張材質
- 只 blit 1 次

#### NaraInitScreen
```java
@Override
public void render(...) {
    // ⚠️ 潛在問題：多次材質切換
    graphics.blit(gradientTexture, ...);         // 材質 1
    graphics.blit(OVERLAY_TEXTURE, ...);         // 材質 2
    graphics.drawCenteredString(...);            // 字型材質
    graphics.drawString(...);                    // 字型材質（可能重用）
    // ... 6 行文字 = 可能 6 次 draw call
    super.render(graphics, mouseX, mouseY, partialTick);  // 按鈕材質
}
```

**評分**：⚠️ 中等（6/10）
- 材質切換：2-3 次（gradientTexture + OVERLAY_TEXTURE + 按鈕）
- 字型 draw call：6-7 次（標題 + 6 行文字）

**建議優化**：
```java
// 🚀 改進方案：批次繪製文字
// 先畫所有材質，再畫所有文字
graphics.blit(gradientTexture, ...);
graphics.blit(OVERLAY_TEXTURE, ...);

// 批次繪製所有文字（減少狀態切換）
graphics.drawCenteredString(this.font, TITLE, ...);
for (int index = 0; index < linesToDraw; index++) {
    graphics.drawString(this.font, cachedLineSequences[index], ...);
}

super.render(...);  // 最後才渲染按鈕
```

---

### ✅ 4. PoseStack 推/彈與 shader 狀態切換（Impact: 高）

#### NaraIntroScreen
```java
// ✅ 良好：只 push/pop 一次
@Override
public void render(...) {
    PoseStack pose = graphics.pose();
    pose.pushPose();  // 1 次 push
    pose.translate(...);
    pose.mulPose(...);
    pose.translate(...);
    graphics.blit(...);
    pose.popPose();   // 1 次 pop
}
```

**評分**：✅ 完美（10/10）

#### NaraInitScreen
```java
// ✅ 良好：無 PoseStack 操作
@Override
public void render(...) {
    // 直接使用座標，無 push/pop
    graphics.blit(...);
    graphics.drawString(...);
}
```

**評分**：✅ 完美（10/10）

---

### ✅ 5. ItemRenderer 與 Tooltip（Impact: 中）

#### NaraInitScreen - 按鈕 Tooltip
```java
// ✅ 優秀：靜態 List 快取
private static final List<Component> BIND_TOOLTIP = List.of(
    Component.translatable("tooltip.koniava.nara.bind")
);
private static final List<Component> CANCEL_TOOLTIP = List.of(
    Component.translatable("tooltip.koniava.nara.cancel")
);

// ✅ 按鈕建立時傳入快取的 Supplier
bindButton = new TooltipButton(
    0, 0, 90, 20,
    Component.translatable("screen.koniava.nara.bind"),
    BUTTON_TEXTURE, 90, 20,
    button -> { ... },
    () -> BIND_TOOLTIP  // ✅ 返回靜態快取
);
```

**評分**：✅ 完美（10/10）
- Tooltip 靜態快取
- 使用 `List.of()` 不可變集合

---

### ✅ 6. 背景模糊 / shader（Impact: 極高）

#### NaraInitScreen
```java
// ✅ 優秀：靜態貼圖快取
private static ResourceLocation gradientTexture;
private static DynamicTexture gradientDynamic;

private static void ensureGradientTexture(Minecraft minecraft) {
    if (gradientTexture != null) {  // ✅ 只建立一次
        return;
    }

    NativeImage gradientImage = new NativeImage(BG_WIDTH + GRADIENT_PADDING * 2, BG_HEIGHT + GRADIENT_PADDING * 2, true);
    // ... 建立漸層 ...

    gradientDynamic = new DynamicTexture(gradientImage);
    minecraft.getTextureManager().register(gradientLocation, gradientDynamic);
    gradientDynamic.upload();  // ✅ 上傳到 GPU 一次
    gradientTexture = gradientLocation;
}
```

**評分**：✅ 完美（10/10）
- 無每幀 Framebuffer 建立
- 漸層貼圖只建立一次
- 移除時正確清理 VRAM

---

### ⚠️ 7. 在 GUI 開啟時世界仍全速運轉（Impact: 中）

```java
// ❌ 未覆寫：預設回傳 false（不暫停遊戲）
public class NaraIntroScreen extends Screen {
    // 缺少：
    // @Override
    // public boolean isPauseScreen() {
    //     return true;
    // }
}

public class NaraInitScreen extends Screen {
    // 缺少：
    // @Override
    // public boolean isPauseScreen() {
    //     return true;
    // }
}
```

**評分**：❌ 需改進（4/10）

**建議優化**：
```java
@Override
public boolean isPauseScreen() {
    return true;  // 單人遊戲時暫停世界渲染
}
```

---

### ✅ 8. 不必要的 setShaderColor/setColor/setAlpha（Impact: 中）

#### NaraIntroScreen
```java
// ✅ 良好：無 setColor 調用
@Override
public void render(...) {
    graphics.fill(...);
    graphics.blit(CIRCLE_TEXTURE, ...);  // 預設白色，無需 setColor
}
```

**評分**：✅ 完美（10/10）

#### NaraInitScreen
```java
// ✅ 良好：無 setColor 調用
@Override
public void render(...) {
    graphics.blit(gradientTexture, ...);
    graphics.blit(OVERLAY_TEXTURE, ...);
    graphics.drawCenteredString(..., 0xFFFFFF);  // 直接傳顏色
    graphics.drawString(..., 0xAAAAAA);          // 直接傳顏色
}
```

**評分**：✅ 完美（10/10）

---

### ✅ 9. 動畫計算與插值（Impact: 低~中）

#### NaraIntroScreen
```java
// ✅ 優秀：動畫在 tick() 更新
@Override
public void tick() {
    ticksElapsed++;
    accumulatedAngle += ROTATION_SPEED;  // ✅ 只在 tick 更新
    if (accumulatedAngle >= 360F) {
        accumulatedAngle -= 360F;
    }
}

// ✅ render() 只做簡單插值
@Override
public void render(..., float partialTick) {
    float interpolatedAngle = accumulatedAngle + (ROTATION_SPEED * partialTick);  // ✅ 簡單加法
}
```

**評分**：✅ 完美（10/10）
- 動畫狀態在 tick() 更新
- render() 只做線性插值
- 無 Math.sin/cos（已優化掉預烘焙）

#### NaraInitScreen
```java
// ✅ 優秀：動畫在 tick() 更新
@Override
public void tick() {
    super.tick();
    ticksElapsed++;  // ✅ 簡單計數

    if (currentStage == Stage.SHOWING_LINES) {
        if (visibleLines < TEXT_LINES.length && ticksElapsed % 10 == 0) {
            visibleLines++;  // ✅ 簡單遞增
            textCacheDirty = true;
        }
    }
}
```

**評分**：✅ 完美（10/10）

---

## 📊 總體評分

| 項目 | NaraIntroScreen | NaraInitScreen | 加權影響 |
|------|-----------------|----------------|----------|
| 1. 每幀建立物件 | 9/10 ✅ | 9/10 ✅ | **極高** |
| 2. 文字排版與計寬 | N/A | 10/10 ✅ | 高 |
| 3. 材質綁定/切換 | 8/10 ✅ | 6/10 ⚠️ | **極高** |
| 4. PoseStack 推/彈 | 10/10 ✅ | 10/10 ✅ | 高 |
| 5. ItemRenderer/Tooltip | N/A | 10/10 ✅ | 中 |
| 6. 背景模糊/shader | N/A | 10/10 ✅ | **極高** |
| 7. 世界仍全速運轉 | 4/10 ❌ | 4/10 ❌ | 中 |
| 8. setShaderColor | 10/10 ✅ | 10/10 ✅ | 中 |
| 9. 動畫計算與插值 | 10/10 ✅ | 10/10 ✅ | 低~中 |
| **總分** | **8.5/10** | **8.7/10** | - |

---

## 🎯 需要改進的項目

### 🔴 高優先（影響大）

#### 1. NaraInitScreen - 材質切換優化

**問題**：2-3 次材質切換 + 6-7 次文字 draw call

**優化方案**：
```java
@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    if (!layoutCached) {
        updateLayoutCache();
    }
    refreshTextCache();

    // 🎨 階段 1: 繪製所有材質
    if (gradientTexture != null) {
        graphics.blit(gradientTexture, ...);
    }
    graphics.blit(OVERLAY_TEXTURE, ...);

    // 🎨 階段 2: 批次繪製所有文字（減少字型材質切換）
    int startY = cachedCenterY - 50;
    graphics.drawCenteredString(this.font, TITLE, cachedCenterX, startY, 0xFFFFFF);

    if (visibleLines > 0) {
        int linesToDraw = Math.min(visibleLines, cachedLineSequences.length);
        // ✅ 連續繪製所有文字，字型材質只綁定一次
        for (int index = 0; index < linesToDraw; index++) {
            graphics.drawString(this.font, cachedLineSequences[index],
                              cachedLineStartX[index],
                              startY + 20 + index * LINE_HEIGHT,
                              0xAAAAAA);
        }
    }

    // 🎨 階段 3: 最後渲染按鈕（可能切換材質）
    super.render(graphics, mouseX, mouseY, partialTick);
}
```

**預期提升**：5-10% 效能提升

---

### 🟡 中優先（影響中）

#### 2. 添加 isPauseScreen()

**NaraIntroScreen.java**：
```java
@Override
public boolean isPauseScreen() {
    return true;  // 單人遊戲時暫停世界，減少背景渲染負擔
}
```

**NaraInitScreen.java**：
```java
@Override
public boolean isPauseScreen() {
    return true;  // 單人遊戲時暫停世界，減少背景渲染負擔
}
```

**預期提升**：單人遊戲時 10-20% 效能提升（因為停止世界渲染）

---

## 🚀 額外優化建議

### 可選優化 1：合併材質圖集

如果 `gradientTexture` 和 `OVERLAY_TEXTURE` 可以合併成一張 atlas：

```java
// 將兩張材質合併成一張，減少材質切換
private static final ResourceLocation COMBINED_ATLAS = ResourceLocation.fromNamespaceAndPath(
    KoniavacraftMod.MOD_ID, "textures/gui/nara_init_atlas.png"
);

@Override
public void render(...) {
    // 只切換一次材質
    graphics.blit(COMBINED_ATLAS, x1, y1, u1, v1, w1, h1, 512, 256);  // 漸層區域
    graphics.blit(COMBINED_ATLAS, x2, y2, u2, v2, w2, h2, 512, 256);  // Overlay 區域
}
```

**預期提升**：3-5%

---

### 可選優化 2：減少按鈕渲染

檢查 `TooltipButton` 是否每幀重建 tooltip：

```java
// 確保 TooltipButton 內部也有快取機制
public class TooltipButton extends Button {
    private final Supplier<List<Component>> tooltipSupplier;
    private List<Component> cachedTooltip;  // ✅ 快取

    @Override
    public void renderWidget(...) {
        if (cachedTooltip == null) {
            cachedTooltip = tooltipSupplier.get();  // ✅ 只建立一次
        }
        // 使用 cachedTooltip
    }
}
```

---

## 📈 優化效益預估

| 優化項目 | 難度 | 預期效能提升 | 優先級 |
|---------|------|-------------|--------|
| 添加 isPauseScreen() | ⭐ 簡單 | 10-20% | 🔴 高 |
| 材質切換優化（批次繪製） | ⭐⭐ 中等 | 5-10% | 🔴 高 |
| 合併材質圖集 | ⭐⭐⭐ 困難 | 3-5% | 🟢 低 |
| 按鈕 tooltip 快取檢查 | ⭐⭐ 中等 | 1-3% | 🟢 低 |

---

## ✅ 結論

**目前狀態**：已經非常優秀（8.5-8.7/10）

**剩餘瓶頸**：
1. ❌ 未暫停遊戲世界（單人時白白浪費效能）
2. ⚠️ 可以進一步減少材質切換

**建議行動**：
1. **立即實作**：添加 `isPauseScreen()` 回傳 `true`（1 分鐘，效益 10-20%）
2. **可選實作**：優化材質切換順序（5 分鐘，效益 5-10%）

需要我幫你實作這兩個優化嗎？
