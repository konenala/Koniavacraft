# Arcane Pedestal 渲染問題診斷

> 問題：放置物品到奧術基座上，但看不到浮動的物品渲染

---

## ✅ 已確認正常的部分

### 1. 渲染器已註冊
`ModRenderLayers.java:23`
```java
event.registerBlockEntityRenderer(ModBlockEntities.ARCANE_PEDESTAL_BE.get(), ArcanePedestalRenderer::new);
```

### 2. Block 設定正確
- ✅ `getRenderShape()` 回傳 `RenderShape.INVISIBLE`（讓 TESR 接管渲染）
- ✅ `getTicker()` 正確註冊客戶端與伺服端 tick
- ✅ `newBlockEntity()` 正確創建 BlockEntity

### 3. BlockEntity 邏輯正常
- ✅ `getOffering()` 方法存在
- ✅ `getSpinForRender()` 方法存在
- ✅ `getHoverOffset()` 方法存在
- ✅ `isOfferingConsumed()` 方法存在
- ✅ 客戶端 tick 會更新 `spin` 角度
- ✅ NBT 同步正確實作（`getUpdatePacket()`, `onDataPacket()`）

### 4. 渲染器邏輯正常
- ✅ 第 189-192 行：檢查 `!offering.isEmpty()` 並呼叫 `renderOffering()`
- ✅ 第 310 行：物品位置在 `(0.5, 2.0, 0.5)` 方塊頂部
- ✅ 第 312-316 行：浮動與旋轉動畫
- ✅ 第 326-335 行：使用 `itemRenderer.renderStatic()` 渲染物品

---

## 🔍 可能的問題

### 問題 1：ItemRenderer 未初始化 ❌
**檢查點**：`ArcanePedestalRenderer.java:47`
```java
this.itemRenderer = context.getItemRenderer();
```
**可能原因**：`context` 可能為 null 或未提供 ItemRenderer

---

### 問題 2：物品未同步到客戶端 ⚠️
**症狀**：伺服端有物品，但客戶端沒收到

**檢查方法**：
1. 在客戶端打開 F3 debug 畫面
2. 查看 BlockEntity 數據是否包含物品

**可能原因**：
- `setChangedAndSync()` 未正確呼叫
- 網路封包未發送

**解決方案**：在 `ArcanePedestalBlockEntity.java:306-312` 檢查：
```java
private void setChangedAndSync() {
    setChanged();
    if (level != null && !level.isClientSide) {
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 3);  // ✅ 標記 3 = 同步客戶端
    }
}
```

---

### 問題 3：渲染位置超出視野 ⚠️
**症狀**：物品被渲染了，但在玩家看不到的地方

**檢查點**：`ArcanePedestalRenderer.java:310`
```java
poseStack.translate(0.5D, 2.0D, 0.5D); // Y=2.0 可能太高？
```

**測試方案**：
1. 臨時改成 `Y=1.0`
2. 重新編譯測試
3. 如果能看到物品，就是位置問題

---

### 問題 4：模型載入失敗導致整個渲染中斷 ❌
**檢查點**：`ArcanePedestalRenderer.java:184-186`
```java
if (modelLoaded) {
    renderBlockModel(...);
}
```

**可能原因**：
- JSON 模型檔案不存在：`models/block/arcane_pedestal.json`
- 材質檔案不存在：`textures/block/arcane_pedestal_texture.png`
- JSON 解析錯誤

**測試方案**：
1. 檢查遊戲日誌是否有 `Failed to load Arcane Pedestal model` 錯誤
2. 如果有錯誤，可能導致整個渲染器崩潰

**臨時解決**：註解掉方塊模型渲染，只測試物品：
```java
// if (modelLoaded) {
//     renderBlockModel(...);
// }
```

---

### 問題 5：光照值為 0 導致物品全黑 ⚠️
**檢查點**：`ArcanePedestalRenderer.java:329`
```java
itemRenderer.renderStatic(
    offering,
    ItemDisplayContext.GROUND,
    packedLight,  // ⚠️ 這個值可能是 0
    packedOverlay,
    poseStack,
    bufferSource,
    blockEntity.getLevel(),
    0
);
```

**測試方案**：強制使用最大光照：
```java
int fullBright = 0xF000F0; // 15 級天空光 + 15 級方塊光
itemRenderer.renderStatic(
    offering,
    ItemDisplayContext.GROUND,
    fullBright,  // 改用固定亮度
    packedOverlay,
    poseStack,
    bufferSource,
    blockEntity.getLevel(),
    0
);
```

---

## 🧪 診斷步驟

### 步驟 1：檢查遊戲日誌
```bash
# 啟動遊戲並放置物品到基座
# 查看是否有這些錯誤：
# - "Failed to load Arcane Pedestal model"
# - NullPointerException
# - 其他渲染相關錯誤
```

### 步驟 2：添加除錯日誌
在 `ArcanePedestalRenderer.java:179` 添加：
```java
@Override
public void render(ArcanePedestalBlockEntity blockEntity, float partialTick,
                   PoseStack poseStack, MultiBufferSource bufferSource,
                   int packedLight, int packedOverlay) {

    // 🐛 除錯日誌
    ItemStack offering = blockEntity.getOffering();
    if (!offering.isEmpty()) {
        KoniavacraftMod.LOGGER.info("Rendering offering: {} at position {}",
            offering.getItem(), blockEntity.getBlockPos());
    }

    // ... 原本的代碼
}
```

### 步驟 3：測試簡化版本
暫時註解掉方塊模型渲染：
```java
@Override
public void render(ArcanePedestalBlockEntity blockEntity, float partialTick,
                   PoseStack poseStack, MultiBufferSource bufferSource,
                   int packedLight, int packedOverlay) {

    // 🎨 渲染方塊模型
    // if (modelLoaded) {
    //     renderBlockModel(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    // }

    // 🎁 渲染祭品物品（只測試這個）
    ItemStack offering = blockEntity.getOffering();
    if (!offering.isEmpty()) {
        renderOffering(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, offering);
    }
}
```

### 步驟 4：使用固定光照測試
在 `renderOffering()` 方法中：
```java
private void renderOffering(ArcanePedestalBlockEntity blockEntity, float partialTick,
                            PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay, ItemStack offering) {
    poseStack.pushPose();
    poseStack.translate(0.5D, 1.0D, 0.5D); // 改低一點測試

    // 移除所有動畫，簡化測試
    // float hoverOffset = blockEntity.getHoverOffset(partialTick);
    // poseStack.translate(0.0D, hoverOffset, 0.0D);
    // float rotation = blockEntity.getSpinForRender(partialTick);
    // poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

    poseStack.scale(0.5f, 0.5f, 0.5f);

    int fullBright = 0xF000F0; // 固定最亮
    itemRenderer.renderStatic(
        offering,
        ItemDisplayContext.GROUND,
        fullBright,  // 使用固定光照
        packedOverlay,
        poseStack,
        bufferSource,
        blockEntity.getLevel(),
        0
    );

    poseStack.popPose();
}
```

---

## 🎯 最可能的問題

根據程式碼分析，**最可能的問題是**：

### 1️⃣ 光照值為 0（60% 可能性）
- 症狀：物品被渲染了，但全黑看不見
- 解決：改用固定光照 `0xF000F0`

### 2️⃣ 模型載入失敗導致渲染器崩潰（30% 可能性）
- 症狀：整個 `render()` 方法沒被執行
- 解決：檢查日誌，註解掉方塊模型渲染

### 3️⃣ 物品未同步到客戶端（10% 可能性）
- 症狀：伺服端有物品，客戶端沒有
- 解決：檢查 F3 debug，確認 NBT 同步

---

## 🔧 快速修復建議

立即在 `renderOffering()` 方法添加這兩行：

```java
private void renderOffering(...) {
    poseStack.pushPose();
    poseStack.translate(0.5D, 1.0D, 0.5D); // 改低到 Y=1.0

    // ... 省略動畫代碼 ...

    int fullBright = 0xF000F0; // 🔧 強制使用最大光照
    itemRenderer.renderStatic(
        offering,
        ItemDisplayContext.GROUND,
        fullBright,  // 🔧 改這裡
        packedOverlay,
        poseStack,
        bufferSource,
        blockEntity.getLevel(),
        0
    );

    poseStack.popPose();
}
```

如果這樣能看到物品，就代表是光照問題。

---

需要我直接幫你修改代碼測試嗎？
