# 系統規格說明

## 1. 架構與選型
- **平台**：Minecraft NeoForge 1.21.1，採用 Java 21 與 Gradle Wrapper。
- **模組結構**：核心代碼位於 `src/main/java/com/github/nalamodikk/`，依註冊類別（Blocks、Items、BlockEntities 等）劃分套件。
- **工具鏈**：Gradle 用於建構、資料生成與測試；資料資源透過 NeoForge Data Generator 產生至 `src/generated/resources/`。
- **環境需求**：必須安裝 JDK 21 並設定系統或使用者層級 `JAVA_HOME`，確保 `powershell.exe -Command "java -version"` 可正確回報版本；缺少設定時 `runData` 將無法啟動。
- **文件管理**：協作指南整併為單一內容，分別存放於根目錄 `AGENTS.md` 與 `docs/AGENTS.md`，規格文件（spec.md, api.md）與其他敘述性文件位於 `docs/` 資料夾。

## 2. 資料模型
- **方塊註冊**：`ModBlocks` 使用 NeoForge `DeferredRegister` 維護 `RegistryObject<Block>`。
- **物品註冊**：`ModItems` 管理 `RegistryObject<Item>`，與方塊對應或提供獨立功能。
- **能量系統**：`ManaStorage` 與 `ModNeoNalaEnergyStorage` 儲存雙能量值（魔力 / RF），提供 `receive`, `extract`, `getMana` 介面。
- **網路節點**：導管類方塊藉由 `NetworkManager` 維護節點狀態、IO 型態與相鄰連線。
- **戰利品表**：粉筆符文方塊 `chalk_glyph` 需對應 `data/koniava/loot_tables/blocks/chalk_glyph.json`，確保資料生成與遊戲內掉落一致。
- **儀式基座**：`common.block.blockentity.ritual.ArcanePedestalBlockEntity` 維護單一祭品槽、消耗狀態與動畫參數（旋轉 / 浮動），伺服端透過 `setChangedAndSync()` 同步庫存與狀態，並提供 `serverTick` / `clientTick` 介面供儀式核心與渲染器呼叫。
  - 空祭品狀態不寫入 NBT；同步封包應透過 `saveOptional` 或條件判斷避免序列化空堆疊，防止服務端崩潰。
  - 粉筆紋理統一存於 `textures/block/ritual/` 子資料夾，資料生成需透過 `modLoc("block/ritual/...")` 讀取，避免與一般方塊資源混淆。
- **Nara UI**：`narasystem.nara.screen.NaraIntroScreen` 與 `NaraInitScreen` 為客戶端過場 / 綁定流程，採用快取佈局與節奏驅動動畫；所有貼圖在 `init()` 先設定過濾方式，並需避免逐幀建立 Widget 與重複轉換矩陣以降低 GPU 負載。
- **儀式配方**：`RitualRecipe.matches` 會以 `RitualValidationContext` 的 `structureSummary` 對照 `structure_requirements`，逐鍵驗證基座數量、魔力塔距離與粉筆顏色／圖案條件，不符時回傳對應錯誤訊息。
- **粉筆顏色枚舉**：`ChalkGlyphBlock.ChalkColor` 將顏色名稱改為語系鍵 `color.koniavacraft.chalk.<color>`，並透過 `Component.translatable` 返回顯示用文字，供訊息與 tooltip 共用。

- **儀式方塊族群**：`common.block.ritualblock` 與 `common.block.blockentity.ritual` 維護 Ritual Core、Arcane Pedestal、Rune Stone、Mana Pylon 與 Chalk Glyph；BlockEntity 提供儀式進度、魔力儲量、祭品槽與符文加成資料，詳見 `個人開發者小記錄/ritual/祭壇符文系統.md`。
  - `validator` 子套件提供 `RitualStructureValidator` / `RitualMaterialValidator`，責任劃分為幾何佈局與祭品清單檢查；`RitualCoreBlockEntity` 僅負責狀態機。結構驗證現包括：
    - 掃描核心六格半徑內的基座並確認各自位於四向固定偏移，同時檢查 `ArcanePedestalBlock` 面向是否指向核心；違規時回報 `message.koniavacraft.ritual.error.pedestal_facing`。
    - 收集魔力塔並使用 `pedestal.total`、`pedestal.north/south/east/west`、`pylon.total` 等鍵填入 `structureSummary`，配方可透過 `structure_requirements` 要求最小數量，魔力塔距離超過 9 格會觸發 `message.koniavacraft.ritual.error.pylon_distance`。
    - 掃描八格內的粉筆符號，統計 `glyph.color.<color>` 與 `glyph.pattern.<pattern>` 計數，至少要求存在粉筆並於缺漏時回報 `message.koniavacraft.ritual.error.no_glyph`；所有結果寫入 `structureSummary` 供配方使用。
    - 將統計輸出傳遞給 `RitualMaterialValidator`，由 `RitualRecipe.matches` 依 `structure_requirements` 逐鍵檢查需求，缺失時補充 `message.koniavacraft.ritual.error.structure_requirement`。
  - `RitualCoreTracker` 快取核心座標，基座與符文改用追蹤器查詢核心，基座在祭品變動時呼叫 `onPedestalContentsChanged()` 強制重新評估。
  - `RitualRecipe` 只保留內建 `Serializer`，配方序列化/反序列化集中於單一路徑維護。

## 3. 關鍵流程
1. 遊戲啟動時進行所有內容註冊（Blocks、Items、BlockEntities、Menus、Recipes）。
2. 執行資料生成前，需先以 `powershell.exe -Command "java -version"` 確認 JDK 21 與 `JAVA_HOME` 設定無誤，並確認 `data/koniava/loot_tables/blocks/chalk_glyph.json` 存在，再透過 `./gradlew runData` 重建 JSON 配方、模型。
3. 方塊實體 `tick` 中與能量儲存、網路傳輸交互，更新 GUI 與同步封包。
4. Arcane Pedestal 伺服端 `serverTick` 處理粒子與儀式核心的互動，客戶端 `clientTick` 更新旋轉與浮動動畫，任何庫存／狀態變化需透過 `setChangedAndSync()` 通知客戶端，避免渲染器取得舊值。
5. `RitualCoreBlockEntity` 在 `PREPARING` 狀態委派 `RitualStructureValidator` / `RitualMaterialValidator`；結構驗證會檢查基座位置與朝向、魔力塔距離、粉筆顏色與圖案統計，並輸出 `structureSummary`（含 `pedestal.*`、`pylon.*`、`glyph.color.*`、`glyph.pattern.*`），材料驗證依 `structure_requirements` 逐鍵比對，不符時將錯誤寫入 `RitualValidationContext` 並提示玩家。
6. Arcane Pedestal 祭品變更時會透過 `RitualCoreTracker` 通知核心，核心於 `RUNNING`/`PREPARING` 狀態下自動中止並提示。
7. 同步封包前先檢查祭品是否為空堆疊，僅在有物品時寫入 NBT，避免 `ItemStack.save` 對空堆疊編碼導致崩潰。
8. 文檔維運流程：更新 `spec.md` → 更新 `api.md` → 同步根目錄與 `docs/AGENTS.md` 的協作指南 → 更新 `todolist.md`。
9. 語系資源維護：比對 `assets/koniava/lang/en_us.json` 與 `zh_tw.json`，補齊儀式錯誤與提示訊息，避免硬編碼中文。
   - 翻譯對照：`block.koniava.chalk_glyph` →「粉筆符號」、`block.koniava.rune_stone_efficiency` →「效率符文石」、`block.koniava.rune_stone_stability` →「穩定符文石」、`block.koniava.rune_stone_augmentation` →「增幅符文石」、`block.koniava.rune_stone_celerity` →「迅捷符文石」。
10. 儀式資產維護：將 `個人開發者小記錄/ritual` 下的預覽材質依規格命名並紀錄缺漏項目，確保後續匯入遊戲資源時能直接對應。共鳴水晶物品模型需改用專用貼圖並維持 GUI 0.9 / 掉落 0.8 的縮放，避免放大產生的外殼視覺。
11. 資產套用稽核：比對程式註冊（Blocks/Items）、模型、材質與資料生成輸出，確保新資產均已連結對應檔案並記錄缺失。
12. 儀式資產整合：將檔案從 `個人開發者小記錄/ritual` 搬運至正式資源路徑（`textures`, `models`, `blockstates`），並以資料生成驗證輸出。
13. 粉筆資產補完：自動化產生缺漏粉筆圖案（以白色圖案套色）、補齊彩色粉筆物品模型並同步資料生成。
14. Nara UI 優化：於畫面初始化緩存佈局與貼圖狀態，渲染循環僅進行必要變換，避免造成 GPU 過載。
15. Chalk Glyph 紋理維護：資源集中於 `textures/block/ritual`，更新貼圖時需同步調整資料生成路徑。

## 4. 虛擬碼
```pseudo
function maintainDocumentation(change):
    reviewProjectStructure()
    updateSpec(change.summary)
    updateApi(change.externalImpact)
    syncContributorGuide(change.instructions)
    recordTasksInTodo()
```

## 5. 系統脈絡圖
- **外部角色**：玩家（操作模組內容）、協作者（維護代碼與文件）。
- **主要系統**：Koniavacraft 模組。
- **外部系統**：Minecraft 客戶端 / 伺服器、Gradle 建構鏈。
- **互動**：協作者透過 Git 與文檔維護；玩家於遊戲中與模組功能互動。

## 6. 容器 / 部署概觀
- **客戶端容器**：Minecraft Client，載入模組 JAR，提供 GUI 與可視化效果。
- **伺服器容器**：Minecraft Server，維護方塊實體、網路狀態與同步。
- **開發容器**：Gradle 任務（`runClient`, `runServer`, `runData`）提供快速迭代環境。

## 7. 模組關係圖（Backend / Frontend）
- **前端（Client）**：`client/screenapi`、GUI Render Utils 負責畫面呈現與交互。
- **後端（Server）**：`common/block/blockentity`、`common/network` 處理邏輯與資料流。
- **共用層**：`register`, `common/utils`, `data` 提供註冊與工具方法。

## 8. 序列圖
1. 玩家操作 GUI → 客戶端 Screen 發送封包。
2. 伺服器 BlockEntity 接收封包 → 更新能量儲存或網路設定。
3. BlockEntity 回傳狀態封包 → 客戶端刷新畫面。
4. Arcane Pedestal 觸發 `setChangedAndSync()` → 客戶端接收更新後渲染器讀取最新祭品與動畫參數。
5. 文檔變更 → 觸發 spec/api/協作指南同步更新。
6. 語系檔案審查 → 確認中英翻譯一致後回寫繁中內容。
7. 儀式資產命名 → 將最新預覽素材整理為對應檔名並同步待辦紀錄。
8. 資產套用稽核 → 逐項確認註冊物件與資產檔案對應情況，整理缺失報告。
9. 儀式資產整合 → 將正式資源同步至主資料夾並進行資料生成檢查。
10. 粉筆資產補完 → 產生缺漏圖案並更新彩色粉筆物品模型。

## 9. ER 圖
- **Block** (block_id, display_name) 1..* ←→ 1..* **BlockEntity** (entity_id, energy_capacity, io_mode)。
- **Item** (item_id, category) 與 Block 0..1 → 1 關聯。
- **NetworkNode** (node_id, block_pos, io_type) 與 BlockEntity 1 → * 關聯。

## 10. 類別圖（後端關鍵類別）
- `BlockEntity` ← `ManaGeneratorBlockEntity`, `ManaInfuserBlockEntity`, `ConduitBlockEntity`, `ArcanePedestalBlockEntity`（ritual 套件版本）。
- `NetworkManager` 聚合 `NetworkNode`、`TransferManager`，並依據 IO 設定進行更新。
- `ManaStorage` 被上述 BlockEntity 組合使用，提供能量操作介面。

## 11. 流程圖
```
開始 → 需求確認 → 更新 spec.md → 更新 api.md → 同步 AGENTS.md (根目錄 / docs) → 更新待辦 → 語系檔案審查與翻譯 → 儀式資產命名 → 資產套用稽核 → 儀式資產整合 → 粉筆資產補完 → 自檢 → 結束
```

## 12. 狀態圖
- 文檔狀態：`草稿` → `待審閱` → `已發佈`。
- 若檢查發現缺漏則回退至 `草稿`，完成審閱後進入 `已發佈`。
