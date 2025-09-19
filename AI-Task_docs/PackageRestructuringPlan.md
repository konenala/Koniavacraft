# 包結構重組建議

## 當前包結構問題

1. **混亂的包組織** - 功能相似的類分散在不同包中
2. **深層嵌套** - 某些包路徑過深，影響可讀性
3. **命名不一致** - 包名和類名沒有遵循統一約定
4. **功能交叉** - 相關功能沒有合理分組

## 建議的新包結構

```
com.github.nalamodikk/
├── core/                          # 核心系統
│   ├── KoniavacraftMod.java      # 主mod類
│   ├── config/                    # 配置系統
│   │   ├── ModCommonConfig.java
│   │   └── BiomeConfig.java
│   └── constants/                 # 常量定義
│       ├── ModConstants.java
│       └── ConfigDefaults.java
│
├── registry/                      # 註冊系統 (重命名自register)
│   ├── ModBlocks.java
│   ├── ModItems.java
│   ├── ModBlockEntities.java
│   ├── ModCapabilities.java
│   ├── ModMenuTypes.java
│   ├── ModRecipes.java
│   ├── ModDataAttachments.java
│   └── ModCreativeModTabs.java
│
├── blocks/                        # 方塊系統
│   ├── ModBlocks.java             # 方塊註冊 (如果需要分離)
│   ├── base/                      # 基礎方塊類
│   │   ├── AbstractManaBlock.java
│   │   └── AbstractMachineBlock.java
│   ├── machines/                  # 機器方塊
│   │   ├── ManaGeneratorBlock.java
│   │   ├── ManaInfuserBlock.java
│   │   └── ModularMachineBlock.java
│   ├── natural/                   # 自然方塊
│   │   ├── ManaOreBlock.java
│   │   ├── ManaSoilBlock.java
│   │   └── ManaGrassBlock.java
│   └── conduits/                  # 導管方塊
│       ├── ManaConduitBlock.java
│       └── ArcaneConduitBlock.java
│
├── blockentities/                 # 方塊實體 (重命名並簡化)
│   ├── base/                      # 基礎BE類
│   │   ├── AbstractManaBlockEntity.java
│   │   └── AbstractMachineBlockEntity.java
│   ├── machines/                  # 機器BE
│   │   ├── ManaGeneratorBlockEntity.java
│   │   ├── ManaInfuserBlockEntity.java
│   │   └── ModularMachineBlockEntity.java
│   ├── conduits/                  # 導管BE
│   │   ├── ManaConduitBlockEntity.java
│   │   └── ArcaneConduitBlockEntity.java
│   └── util/                      # BE工具類
│       ├── SyncHelper.java
│       └── IOConfigHelper.java
│
├── items/                         # 物品系統
│   ├── base/                      # 基礎物品類
│   │   ├── AbstractManaItem.java
│   │   └── AbstractToolItem.java
│   ├── materials/                 # 材料物品
│   │   ├── ManaDustItem.java
│   │   └── ManaIngotItem.java
│   ├── tools/                     # 工具物品
│   │   ├── BasicTechWandItem.java
│   │   └── ManaDebugToolItem.java
│   ├── modules/                   # 模組物品
│   │   ├── BaseModuleItem.java
│   │   └── upgrades/
│   │       ├── SpeedUpgradeItem.java
│   │       └── EfficiencyUpgradeItem.java
│   └── equipment/                 # 裝備物品
│       ├── ManaArmorItem.java
│       └── PoweredArmorItem.java
│
├── capabilities/                  # 能力系統
│   ├── ModCapabilities.java      # 能力註冊
│   ├── mana/                      # 魔力能力
│   │   ├── IUnifiedManaHandler.java
│   │   ├── ManaStorage.java
│   │   └── ManaAction.java
│   ├── energy/                    # 能量能力
│   │   └── ModNeoNalaEnergyStorage.java
│   └── util/                      # 能力工具
│       ├── CapabilityUtils.java
│       └── IOHandlerUtils.java
│
├── machines/                      # 機器邏輯系統
│   ├── base/                      # 基礎機器邏輯
│   │   ├── AbstractMachine.java
│   │   └── MachineState.java
│   ├── generators/                # 發電機邏輯
│   │   ├── ManaGeneratorLogic.java
│   │   ├── FuelHandler.java
│   │   └── OutputHandler.java
│   ├── processing/                # 處理機器邏輯
│   │   ├── ManaInfuserLogic.java
│   │   └── RecipeProcessor.java
│   └── upgrades/                  # 升級系統
│       ├── IUpgradeableMachine.java
│       ├── UpgradeInventory.java
│       └── UpgradeHandler.java
│
├── network/                       # 網絡系統
│   ├── ModNetworking.java         # 網絡註冊
│   ├── packets/                   # 數據包
│   │   ├── base/
│   │   │   └── BasePacket.java
│   │   ├── sync/
│   │   │   ├── ManaDataSyncPacket.java
│   │   │   └── MachineStateSyncPacket.java
│   │   └── action/
│   │       ├── ModeTogglePacket.java
│   │       └── IOConfigPacket.java
│   └── handlers/                  # 網絡處理器
│       ├── ClientPacketHandler.java
│       └── ServerPacketHandler.java
│
├── gui/                           # 圖形界面
│   ├── screens/                   # 屏幕類
│   │   ├── ManaGeneratorScreen.java
│   │   ├── ModularMachineScreen.java
│   │   └── UpgradeScreen.java
│   ├── menus/                     # 菜單類
│   │   ├── ManaGeneratorMenu.java
│   │   └── ModularMachineMenu.java
│   ├── widgets/                   # 自定義組件
│   │   ├── ManaBarWidget.java
│   │   └── IOConfigWidget.java
│   └── util/                      # GUI工具
│       ├── GuiHelper.java
│       └── RenderHelper.java
│
├── recipes/                       # 配方系統
│   ├── ModRecipes.java           # 配方註冊
│   ├── types/                     # 配方類型
│   │   ├── ManaInfusionRecipe.java
│   │   └── FuelRecipe.java
│   ├── serializers/              # 配方序列化
│   │   ├── ManaInfusionRecipeSerializer.java
│   │   └── FuelRecipeSerializer.java
│   └── loaders/                  # 配方加載器
│       ├── ManaGenFuelRateLoader.java
│       └── RecipeDataLoader.java
│
├── worldgen/                     # 世界生成 (重命名自biome)
│   ├── biomes/                   # 生物群系
│   │   ├── ManaPlainsBiome.java
│   │   └── registration/
│   │       ├── BiomeTerrainRegistration.java
│   │       └── UniversalBiomeRegistration.java
│   ├── features/                 # 世界特徵
│   │   ├── ManaOreFeature.java
│   │   └── ManaCrystalFeature.java
│   ├── processors/               # 世界處理器
│   │   └── UniversalBiomeInjector.java
│   └── mixins/                   # 世界生成Mixin
│       ├── OverworldBiomeBuilderMixin.java
│       └── NoiseGeneratorSettingsMixin.java
│
├── data/                         # 數據系統
│   ├── attachments/              # 數據附件
│   │   ├── ModDataAttachments.java
│   │   └── PlayerManaData.java
│   ├── providers/                # 數據提供者
│   │   ├── ModRecipeProvider.java
│   │   └── ModLootTableProvider.java
│   └── tags/                     # 標籤
│       ├── ModBlockTags.java
│       └── ModItemTags.java
│
├── client/                       # 客戶端專用
│   ├── models/                   # 模型
│   │   └── ManaConduitModel.java
│   ├── renderers/                # 渲染器
│   │   ├── ManaConduitRenderer.java
│   │   └── MachineRenderer.java
│   ├── screens/                  # 客戶端屏幕
│   │   └── ConfigScreen.java
│   └── events/                   # 客戶端事件
│       └── ClientEventHandler.java
│
├── integrations/                 # 模組整合
│   ├── jei/                      # JEI整合
│   │   ├── JEIPlugin.java
│   │   └── categories/
│   │       └── FuelCategory.java
│   ├── top/                      # The One Probe整合
│   │   └── TOPProvider.java
│   └── curios/                   # Curios整合
│       └── CuriosIntegration.java
│
├── util/                         # 工具類
│   ├── math/                     # 數學工具
│   │   ├── MathHelper.java
│   │   └── VectorUtils.java
│   ├── text/                     # 文本工具
│   │   ├── TranslationHelper.java
│   │   └── ColorHelper.java
│   ├── nbt/                      # NBT工具
│   │   ├── NBTHelper.java
│   │   └── CompoundHelper.java
│   └── world/                    # 世界工具
│       ├── WorldHelper.java
│       └── ChunkHelper.java
│
└── api/                          # 公共API
    ├── capabilities/             # API能力接口
    │   ├── IManaHandler.java
    │   └── IUpgradeable.java
    ├── machines/                 # API機器接口
    │   ├── IMachine.java
    │   └── IProcessor.java
    └── events/                   # API事件
        ├── ManaEvents.java
        └── MachineEvents.java
```

## 重組步驟建議

### 第一階段：基礎重組
1. **創建新的包結構目錄**
2. **移動註冊類** - 將 `register` 包重命名為 `registry`
3. **重組核心類** - 移動主mod類和配置到 `core` 包
4. **分離工具類** - 創建 `util` 包並移動通用工具

### 第二階段：功能分組
1. **方塊系統重組** - 按功能分組方塊類
2. **BlockEntity重組** - 簡化當前複雜的BE包結構
3. **物品系統重組** - 按類型分組物品類
4. **能力系統整合** - 將所有capability相關類集中

### 第三階段：邏輯分離
1. **機器邏輯提取** - 將BE中的邏輯類提取到獨立包
2. **網絡系統整合** - 整合所有網絡相關類
3. **GUI系統重組** - 分離屏幕、菜單和組件
4. **配方系統整合** - 統一配方相關類

### 第四階段：高級整合
1. **客戶端分離** - 創建專門的客戶端包
2. **API提取** - 創建公共API接口
3. **整合系統** - 組織第三方模組整合
4. **文檔更新** - 更新所有import和引用

## 重組的好處

### 1. **更好的代碼組織**
- 相關功能集中在一起
- 更容易找到特定類
- 減少交叉依賴

### 2. **更好的可維護性**
- 清晰的模塊邊界
- 更容易進行單元測試
- 更容易重構

### 3. **更好的可擴展性**
- 新功能有明確的放置位置
- 更容易添加新的機器類型
- 更容易添加新的能力

### 4. **更好的協作**
- 團隊成員更容易理解結構
- 減少merge衝突
- 更容易code review

## 遷移注意事項

### 1. **向後兼容性**
- 保留關鍵類的包路徑別名
- 逐步遷移，避免一次性大改
- 測試所有功能

### 2. **依賴管理**
- 更新所有import語句
- 檢查反射調用
- 更新資源文件路徑

### 3. **文檔更新**
- 更新README
- 更新開發文檔
- 更新API文檔

## 實施時間表

| 階段 | 預估時間 | 主要工作 |
|------|----------|----------|
| 第一階段 | 1-2天 | 基礎包結構創建和核心類移動 |
| 第二階段 | 2-3天 | 功能分組和類重組 |
| 第三階段 | 3-4天 | 邏輯分離和系統整合 |
| 第四階段 | 2-3天 | 高級功能和文檔更新 |
| **總計** | **8-12天** | **完整重組和測試** |

## 風險評估

### 高風險
- **大量import更改** - 可能導致編譯錯誤
- **資源路徑變更** - 可能影響資源加載
- **反射調用失效** - 需要仔細檢查

### 中風險
- **測試覆蓋** - 需要全面測試所有功能
- **第三方整合** - 可能影響其他mod的兼容性

### 低風險
- **性能影響** - 包結構變更不會影響運行時性能
- **用戶體驗** - 不會影響最終用戶

## 結論

建議按階段進行重組，優先處理最混亂的部分（如當前的blockentity包結構），然後逐步完善整體架構。這樣可以在保證穩定性的同時，大幅提升代碼的可維護性和可讀性。