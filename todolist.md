# Todo List

- [x] 文件同步：更新 spec.md 與 api.md 加入語系維護流程描述（負責：Codex）
- [x] 語系比對：檢查 `assets/koniava/lang/en_us.json` 與 `zh_tw.json` 缺漏（負責：Codex）－未發現缺漏鍵值
- [x] 語系補齊：將缺失鍵值翻譯成繁體中文並寫入 `zh_tw.json`（負責：Codex）－未新增鍵值，已修正重複逗號避免 JSON 解析錯誤
- [x] 圖片盤點：整理 `個人開發者小記錄/ritual` 目錄內資產（負責：Codex）－共計 48 張 PNG（chalk_07~09、sprite_00~45）
- [x] 材質配對：依照 `個人開發者小記錄/代辦清單/儀式系統材質製作清單.md` 描述分類（負責：Codex）－對應完成並標記缺漏/重複

- [x] 材質重命名：將儀式物品與方塊預覽檔對應至規格檔名（負責：Codex）－已依規格命名並將重複檔標記 `_dup_`
- [x] 粉筆符號整理：依顏色與圖案命名 glyph 素材（負責：Codex）－完成 6 色 36+4 預覽命名，保留 alt/缺口記錄


- [x] 資產應用檢查：確認註冊物品/方塊是否已有模型與材質（負責：Codex）－缺失詳列於本次報告
- [x] 數據生成覆蓋：檢查 data/models/recipes 是否已有對應（負責：Codex）－目前未見對應 datagen 輸出

- [x] 儀式素材整合：將 `個人開發者小記錄/ritual` 繪製檔搬入 `assets/koniava/textures` 並移除 `_dup_`（負責：Codex）
- [x] 模型補齊：建立 `models/block`, `models/item`, `blockstates` 缺漏檔（負責：Codex）－透過 datagen provider 新增粉筆/符文配置
- [x] 執行 runData：使用 `./gradlew runData` 產出資料並檢查輸出（負責：Codex）－成功執行，生成粉筆戰利品表，出現白色圓形紋理缺失警告待後續處理

- [x] 補齊粉筆符文戰利品表：新增 `data/koniava/loot_tables/blocks/chalk_glyph.json` 以解除 runData 失敗（負責：Codex，透過 LootTable Provider 補齊）

- [ ] 補齊粉筆白色圓形紋理：新增 `assets/koniava/textures/block/chalk_glyph_white_circle.png` 以消除 runData 警告（待指派）

- [x] Nara Intro/Init Screen 動畫檢查：定位貼圖旋轉停用原因（負責：Codex）－已提供原因與修復建議


- [x] Arcane Pedestal 文檔同步：更新 spec.md / api.md 確認新方塊實體（負責：Codex）
- [x] Arcane Pedestal 崩潰修復：統一方塊實體與渲染註冊（負責：Codex）
- [x] Arcane Pedestal 測試：執行 .\gradlew.bat build 並記錄結果（負責：Codex）
- [x] Arcane Pedestal 空祭品文檔同步：補充空堆疊同步策略（負責：Codex）
- [x] Arcane Pedestal 空祭品崩潰修復：避免序列化空 ItemStack（負責：Codex）
- [x] Arcane Pedestal 空祭品測試：執行 .\gradlew.bat build 與遊戲內重現（負責：Codex）
- [ ] Arcane Pedestal 程式掃描：檢查潛在隱藏錯誤（負責：Codex）
- [x] Nara Screen 文檔同步：更新 spec.md / api.md 記錄 GPU 優化策略（負責：Codex）
- [x] Nara Screen GPU 優化：調整 NaraInitScreen 與 NaraIntroScreen（負責：Codex）
- [ ] Nara Screen 測試：執行 .\gradlew.bat build 並於客戶端驗證（負責：Codex）
- [x] Chalk Glyph 文檔同步：記錄紋理移至 textures/block/ritual（負責：Codex）
- [x] Chalk Glyph 紋理路徑調整：讓程式支援 ritual 子目錄（負責：Codex）
- [ ] Chalk Glyph 測試：執行 runData / build 檢查警告（負責：Codex）
