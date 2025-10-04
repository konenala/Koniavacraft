# Nara UI 超極簡方案

> 回應：「不需要分兩個類」「背景應該用材質而不是渲染」
> 目標：合併成單一類，背景用貼圖

---

## 🎯 進一步簡化建議

### 1. 合併成單一類
```java
// ❌ 目前：兩個類
NaraIntroScreen.java  (68 行) - 開場動畫
NaraInitScreen.java   (148 行) - 初始化畫面

// ✅ 簡化：一個類搞定
NaraScreen.java       (~120 行) - 處理所有邏輯
```

**理由**：
- 兩個畫面只是「階段不同」，不需要分兩個類
- 用 `enum Stage { INTRO, SHOWING_LINES, AWAITING_CONFIRM }` 控制即可
- 減少類間切換（`setScreen(new NaraInitScreen())`）

---

### 2. 背景用貼圖而不是渲染

#### 目前的做法（渲染）
```java
// ❌ 每幀都要調用 fillGradient
graphics.fillGradient(0, 0, this.width, this.height, 0xD0000000, 0x90000000);
```

#### 建議的做法（貼圖）
```java
// ✅ 準備一張 1×256 的漸層貼圖（垂直漸層）
// 檔案：textures/gui/nara_gradient.png
// 尺寸：1px × 256px，從上到下 ARGB(0xD0,0,0,0) → ARGB(0x90,0,0,0)

// 然後用 blit 拉伸渲染
graphics.blit(GRADIENT_TEXTURE, 0, 0, 0, 0, this.width, this.height, 1, 256);
```

**優勢**：
- GPU 直接處理貼圖拉伸（比 fillGradient 快）
- 不需要動態建立 DynamicTexture
- 靜態資源，隨模組打包

---

## 📝 超極簡版代碼

### NaraScreen.java（單一類版本）
```java
package com.github.nalamodikk.narasystem.nara.screen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.screenAPI.component.button.TooltipButton;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.List;

public class NaraScreen extends Screen {
    private enum Stage { INTRO, SHOWING_LINES, AWAITING_CONFIRM }

    // 貼圖資源
    private static final ResourceLocation CIRCLE_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_circle.png");
    private static final ResourceLocation GRADIENT_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_gradient.png");
    private static final ResourceLocation OVERLAY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/nara_overlay.png");
    private static final ResourceLocation BUTTON_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/widget/nara_button.png");

    // 常數
    private static final int INTRO_DURATION = 80;
    private static final int TEX_SIZE = 128;
    private static final float ROTATION_SPEED = 2F;
    private static final int BG_WIDTH = 256;
    private static final int BG_HEIGHT = 190;
    private static final int LINE_HEIGHT = 12;

    // 文字內容
    private static final String MOD_VERSION = ModList.get()
        .getModContainerById(KoniavacraftMod.MOD_ID)
        .map(c -> c.getModInfo().getVersion().toString())
        .orElse("dev");

    private static final Component TITLE = Component.translatable("screen.koniava.nara.title");
    private static final Component[] TEXT_LINES = {
        Component.translatable("screen.koniava.nara.line1"),
        Component.translatable("screen.koniava.nara.line2"),
        Component.translatable("screen.koniava.nara.line3"),
        Component.translatable("screen.koniava.nara.line4"),
        Component.translatable("screen.koniava.nara.line5", MOD_VERSION),
        Component.translatable("screen.koniava.nara.line6")
    };

    private static final List<Component> BIND_TOOLTIP =
        List.of(Component.translatable("tooltip.koniava.nara.bind"));
    private static final List<Component> CANCEL_TOOLTIP =
        List.of(Component.translatable("tooltip.koniava.nara.cancel"));

    // 狀態
    private Stage stage = Stage.INTRO;
    private int ticks = 0;
    private float angle = 0F;
    private int visibleLines = 0;

    private TooltipButton bindButton;
    private TooltipButton cancelButton;

    public NaraScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().getTexture(CIRCLE_TEXTURE).setFilter(false, false);
        mc.getTextureManager().getTexture(OVERLAY_TEXTURE).setFilter(false, false);

        if (stage != Stage.INTRO) {
            createButtons();
        }
    }

    private void createButtons() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 60;

        bindButton = addRenderableWidget(new TooltipButton(
            centerX - 100, buttonY, 90, 20,
            Component.translatable("screen.koniava.nara.bind"),
            BUTTON_TEXTURE, 90, 20,
            btn -> { NaraBindRequestPacket.send(true); onClose(); },
            () -> BIND_TOOLTIP
        ));

        cancelButton = addRenderableWidget(new TooltipButton(
            centerX + 10, buttonY, 90, 20,
            Component.translatable("screen.koniava.nara.cancel"),
            BUTTON_TEXTURE, 90, 20,
            btn -> {
                NaraBindRequestPacket.send(false);
                var conn = Minecraft.getInstance().getConnection();
                if (conn != null) {
                    conn.disconnect(Component.translatable("message.koniava.nara.disconnect_message"));
                }
                onClose();
            },
            () -> CANCEL_TOOLTIP
        ));

        bindButton.visible = (stage == Stage.AWAITING_CONFIRM);
        cancelButton.visible = (stage == Stage.AWAITING_CONFIRM);
    }

    @Override
    public void tick() {
        ticks++;

        if (stage == Stage.INTRO) {
            angle += ROTATION_SPEED;
            if (angle >= 360F) angle -= 360F;

            if (ticks >= INTRO_DURATION) {
                stage = Stage.SHOWING_LINES;
                ticks = 0;
                createButtons();
            }
        } else if (stage == Stage.SHOWING_LINES) {
            if (visibleLines < TEXT_LINES.length && ticks % 10 == 0) {
                visibleLines++;
            }
            if (visibleLines == TEXT_LINES.length && ticks >= TEXT_LINES.length * 10 + 20) {
                stage = Stage.AWAITING_CONFIRM;
                bindButton.visible = true;
                cancelButton.visible = true;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (stage == Stage.INTRO) {
            renderIntro(graphics, partialTick);
        } else {
            renderInit(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderIntro(GuiGraphics graphics, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);

        float interpolatedAngle = angle + (ROTATION_SPEED * partialTick);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.width / 2F, this.height / 2F, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(interpolatedAngle));
        pose.translate(-TEX_SIZE / 2F, -TEX_SIZE / 2F, 0);
        graphics.blit(CIRCLE_TEXTURE, 0, 0, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);
        pose.popPose();
    }

    private void renderInit(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // ✅ 用貼圖渲染背景（拉伸 1×256 的漸層貼圖）
        graphics.blit(GRADIENT_TEXTURE, 0, 0, 0, 0, this.width, this.height, 1, 256);

        int bgX = (this.width - BG_WIDTH) / 2;
        int bgY = (this.height - BG_HEIGHT) / 2;
        graphics.blit(OVERLAY_TEXTURE, bgX, bgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        int startY = centerY - 50;
        graphics.drawCenteredString(this.font, TITLE, centerX, startY, 0xFFFFFF);

        for (int i = 0; i < visibleLines; i++) {
            graphics.drawCenteredString(this.font, TEXT_LINES[i],
                centerX, startY + 20 + i * LINE_HEIGHT, 0xAAAAAA);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return true; }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
```

**行數：~165 行（原 216 行，再減 24%）**

---

## 🖼️ 需要新增的貼圖

### textures/gui/nara_gradient.png

**製作方法 1：使用影像編輯器**
1. 新建圖片：1px × 256px
2. 填充漸層：
   - 上方：RGBA(0, 0, 0, 208) → 十六進制 `#000000D0`
   - 下方：RGBA(0, 0, 0, 144) → 十六進制 `#00000090`
3. 儲存為 PNG

**製作方法 2：使用 Python 腳本**
```python
from PIL import Image

img = Image.new('RGBA', (1, 256))
for y in range(256):
    alpha = int(208 - (208 - 144) * y / 255)
    img.putpixel((0, y), (0, 0, 0, alpha))
img.save('nara_gradient.png')
```

**製作方法 3：我直接幫你生成**
- 讓我用程式碼生成這個貼圖檔案

---

## 📊 最終簡化對比

| 項目 | 原版 | 極簡版 | 超極簡版 |
|------|------|--------|----------|
| **檔案數量** | 2 個 | 2 個 | **1 個** |
| **程式碼行數** | 511 行 | 216 行 | **~165 行** |
| **動態運算** | fillGradient | fillGradient | **貼圖拉伸** |
| **類間切換** | setScreen() | setScreen() | **無** |
| **效能** | 基準 | -1% | **最優** |

---

## 🚀 實作步驟

### 選項 A：完整替換（推薦）
1. 生成 `nara_gradient.png` 貼圖
2. 刪除 `NaraIntroScreen.java` 和 `NaraInitScreen.java`
3. 建立新的 `NaraScreen.java`
4. 更新所有呼叫處（把 `new NaraIntroScreen()` 改成 `new NaraScreen()`）

### 選項 B：保持現狀
如果你覺得分兩個類比較清楚，可以只：
1. 生成 `nara_gradient.png`
2. 在 `NaraInitScreen` 改用貼圖背景

---

需要我：
1. **生成漸層貼圖** (`nara_gradient.png`)
2. **實作超極簡版**（單一類 + 貼圖背景）
3. **只改背景**（保持兩個類，但用貼圖）

選哪一個？
