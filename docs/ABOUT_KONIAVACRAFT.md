# CLAUDE.md

此文件為 Claude Code (claude.ai/code) 在此程式庫中工作時提供指導。

## 建構與開發指令

```bash
# 建構模組
./gradlew build

# 執行開發客戶端
./gradlew runClient

# 執行開發伺服器
./gradlew runServer

# 生成資料（配方、戰利品表、模型）
./gradlew runData

# 執行遊戲測試伺服器
./gradlew runGameTestServer

# 清理建構產物
./gradlew clean
```

## 專案結構與架構

這是 **Koniavacraft**，一個 NeoForge 1.21.1 模組（模組 ID：`koniava`），實作了以魔力為基礎的機器和能量管理系統為中心的魔法工業系統。

### 核心架構模式

**註冊系統**：所有模組內容都採用 NeoForge 的延遲註冊模式，集中化註冊類別位於 `src/main/java/com/github/nalamodikk/register/`：
- `ModBlocks.java` - 方塊註冊
- `ModItems.java` - 物品註冊
- `ModBlockEntities.java` - 方塊實體註冊
- `ModMenuTypes.java` - 容器選單註冊
- `ModRecipes.java` - 配方類型註冊

**機器系統**：方塊實體按功能組織於 `src/main/java/com/github/nalamodikk/common/block/blockentity/`：
- `mana_generator/` - 燃料驅動的魔力/RF 生成
- `mana_infuser/` - 使用魔力強化物品
- `mana_crafting/` - 消耗魔力的自訂合成
- `collector/` - 可再生魔力生成（太陽能收集器）
- `conduit/` - 智慧魔力分配網路

**能力系統**：透過以下方式統一魔力/能量處理：
- `IUnifiedManaHandler` - 主要魔力能力介面
- `ManaStorage` - 單一容器魔力儲存實作
- `ModNeoNalaEnergyStorage` - RF 能量相容性層

### 關鍵組件

**魔力系統**：模組實作雙重能量系統，同時支援自訂魔力和 RF 能量。機器可以同時生成和消耗兩種能量以實現模組生態系統相容性。

**IO 配置**：使用 `IOType` 列舉（INPUT、OUTPUT、BOTH、DISABLED）的精密每面輸入/輸出配置系統。由導管網路用於自動路由。

**網路系統**：奧術導管形成虛擬網路，配備專業管理器：
- `NetworkManager` - 拓撲管理
- `TransferManager` - 魔力傳輸邏輯
- `CacheManager` - 效能最佳化
- `IOManager` - 輸入/輸出協調

**螢幕 API**：位於 `src/main/java/com/github/nalamodikk/client/screenapi/` 的自訂 GUI 框架，具備可拖曳視窗、組件系統和動態工具提示。

### 資料生成

位於 `src/main/java/com/github/nalamodikk/common/datagen/` 的綜合資料生成系統自動生成：
- 方塊/物品模型和方塊狀態
- 戰利品表
- 配方 JSON 檔案
- 標籤和世界生成特徵

執行 `./gradlew runData` 重新生成所有資料檔案到 `src/generated/resources/`。

### JEI 整合

每種配方類型都有專用的 JEI 外掛：
- `ManaCraftingJEIPlugin` - 自訂合成配方
- `ManaInfuserJEIPlugin` - 灌注配方
- 在 JEI 中具有自訂渲染的配方類別

### 開發註記

- 主要模組類別：`KoniavacraftMod.java`（MOD_ID = "koniava"）
- Java 21 目標（在 build.gradle 中配置）
- 使用 Parchment 映射以獲得更好的參數名稱
- Mixins 在 `koniava.mixins.json` 中配置（目前僅有 `OverworldBiomeBuilderMixin`）
- 所有 Java 編譯強制使用 UTF-8 編碼

### 常用工具

位於 `src/main/java/com/github/nalamodikk/common/utils/` 的關鍵工具類別：
- `CapabilityUtils` - 能力查找輔助器
- `GuiRenderUtils` - GUI 渲染工具
- `ItemStackUtils` - ItemStack 操作
- `BlockSelectorUtils` - 方塊選擇和放置
- `NBTJsonConverter` - NBT/JSON 序列化

### 測試與除錯

- 開發模式下可用除錯日誌記錄（`IS_DEV` 標誌）
- 用於測試魔力系統的魔力除錯工具物品
- 已配置遊戲測試框架，但可能需要實作測試
