# Repository Guidelines

## 專案結構與模組組織
Koniavacraft 以 NeoForge 延遲註冊維繫模組化結構。核心程式碼集中於 `src/main/java/com/github/nalamodikk/`，依 `register`（註冊表）、`common`（伺服端/共用邏輯）、`client`（GUI 與渲染）、`network`（封包）區分職責。資產檔案放在 `src/main/resources/`，包含模型、語系、配方與著色器；`src/generated/resources/` 保存資料生成輸出，合併前請以 `git diff` 確認未覆寫手動資產。規格與企劃文件集中在 `docs/`，其中 `spec.md`、`api.md` 與本指南需保持同版內容，必要時於 `AI-Task_docs/` 撰寫設計補充。核心註冊流程由 `KoniavacraftMod` 啟動 DeferredRegister，確保所有內容在正確的模組事件階段載入；暫存世界與測試存於 `run/`，可視需求清除。

## 建構、測試與開發指令
在 PowerShell 中使用 Gradle Wrapper：
- `PowerShell> .\gradlew.bat build`：完整建構並執行靜態檢查，產出於 `build/libs/`；失敗時請先修正再提交。
- `PowerShell> .\gradlew.bat runClient` / `runServer`：啟動測試客戶端與伺服器，用於驗證 GUI、同步與多人互動。
- `PowerShell> .\gradlew.bat runData`：重建 JSON 資產，提交前審閱 `src/generated/resources/` 差異並更新對應說明。
- `PowerShell> .\gradlew.bat runGameTestServer`：執行 NeoForge GameTest，必要時附加 `--debug-jvm` 偵錯。
- `PowerShell> .\gradlew.bat clean`：清除建構暫存，避免舊檔影響測試結果。

## 協作原則與命名風格
- 核心原則：**不要實作未被要求的功能**，先取得確認再行動，並以最小可行版本逐步交付。
- 需求討論優先於編碼；請先敘述設計理念、替代方案與預期視覺化成果，獲得開發者認可後才動手。
- 團隊偏好以可視化成果驅動討論，請優先準備截圖、短片或示意圖佐證想法。
- Java 採四空白縮排；類別 UpperCamelCase、方法/變數 lowerCamelCase、常數 UPPER_SNAKE_CASE；資源檔名小寫加底線。
- 函式必須撰寫繁中註解說明目的、輸入、輸出，重要變數補充用途；請移除 `System.out.println`，改用既有 `LOGGER`。
- GUI 描述優先使用 `translatable` 語系鍵，避免出現硬編碼文字。

## 測試指南
- 測試類別置於 `src/test/java/` 或 GameTest 模組，命名建議 `FeatureNameTests`，並在類別註解說明涵蓋場景。
- 修改方塊實體、網路或 GUI 後，至少執行一次 `runGameTestServer` 與手動 `runClient` 驗證狀態同步、互動邏輯與視覺反饋。
- 若牽涉串流能量或導管拓樸，請補充壓力測試案例（例如多節點拉線）並記錄觀察結果。
- 重新產生資料時，於 PR 說明列出受影響資產及驗證步驟（例：配方能否在工作臺使用），必要時附上世界存檔或截圖。
- 測試需要臨時世界時，建議於 `run/saves/` 建立專用世界並於提交前清除，避免污染共用環境。必要時保留測試紀錄供後續回溯。 Please record test evidence for auditing.

## Commit 與 Pull Request 流程
- Commit 訊息以動詞開頭、50 字元內，可混合中英文，如 `Refine conduit sync timing` 或 `修正 能量 面板 顯示`。
- PR 描述需包含：變更目的、測試步驟與結果、對應議題（`Fixes #編號`）以及 GUI/特效改動之截圖或影片。
- 送審前確認 `spec.md`、`api.md`、`AGENTS.md` 同步更新，並於 `todolist.md` 勾選完成項目以協助審閱。
- 若 PR 涉及資料生成或資產調整，請附上 `git status` 摘要或重點檔案列表，方便檢閱差異。

## 教學互動與回饋
- 回覆需求時採「概念 → 實作 → 優化」三段式說明，確保開發者理解後再進入下一步。
- 遇到設計疑慮應提出至少一個替代方案與利弊分析，並以提問方式確認需求是否調整。
- 若發現規格缺口或世界觀衝突，請先在 `spec.md` 或 `AI-Task_docs/` 補充假設，再同步於 PR 說明。

## 工作流程與安全建議
- 每次開工先 `git pull` 或 `git fetch --all`；長期開發分支請定期 `git rebase main` 保持歷史整潔。
- 需要跨模組協作時，先於 Issue 或規格文件記錄上下游依賴與測試計畫，避免重工。
- 勿提交伺服器位址、金鑰或私人設定；新增第三方模組時，務必於 `README` 與 `gradle.properties` 記錄來源與授權。
- 若任務進度延伸或遇到阻塞，請在 PR 描述或待辦清單留下註記，並在下次同步會議先行報告。
