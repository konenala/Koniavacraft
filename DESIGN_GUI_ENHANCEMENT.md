# GUI 增強系統設計文檔（MIT 授權）

## 📋 目標

替代 LDLib2（GPL）的核心功能，使用 MIT 授權完全重新實現：

1. **雙向數據綁定** - GUI ↔ BlockEntity 自動同步
2. **效能優化** - 減少不必要的網絡流量
3. **RPC 簡化** - 簡化客戶端-服務端通信
4. **自動化註冊** - 反射掃描自動綁定

---

## 🏗️ 核心架構

### 1. 數據綁定系統

#### **新增註解**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BindField {
    /** Widget 的 ID（如果不指定則使用字段名） */
    String value() default "";

    /** 是否雙向綁定（預設 true） */
    boolean twoWay() default true;

    /** 同步間隔（tick），0 表示立即同步 */
    int syncInterval() default 0;
}
```

#### **使用範例**

```java
public class ManaGeneratorBlockEntity extends AbstractManaMachineEntityBlock {

    @BindField("manaBar")  // 自動綁定到 ID 為 "manaBar" 的 Widget
    private int mana;

    @BindField(value = "energyBar", syncInterval = 5)  // 每 5 tick 同步一次
    private int energy;

    @BindField  // 使用字段名 "mode" 作為 Widget ID
    private int mode;
}
```

#### **Screen 中使用**

```java
public class ManaGeneratorScreen extends ModularScreen<ManaGeneratorMenu> {

    @Override
    protected void buildGui(Panel root) {
        // 自動綁定！Widget ID 會自動與 BlockEntity 的 @BindField 匹配
        root.add(new ManaBarWidget(11, 19)
            .setId("manaBar"));  // 自動綁定到 mana 字段

        root.add(new EnergyBarWidget(156, 19)
            .setId("energyBar"));  // 自動綁定到 energy 字段
    }
}
```

---

### 2. RPC 系統

#### **新增註解**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RPC {
    /** RPC 方法名（預設使用方法名） */
    String value() default "";

    /** 執行側：SERVER, CLIENT, BOTH */
    Side side() default Side.SERVER;
}

public enum Side {
    SERVER,   // 只在服務端執行
    CLIENT,   // 只在客戶端執行
    BOTH      // 雙端執行
}
```

#### **使用範例**

**BlockEntity 端**：
```java
public class ManaGeneratorBlockEntity extends AbstractManaMachineEntityBlock {

    @RPC(side = Side.SERVER)
    public void toggleMode() {
        // 自動處理！不需要手寫 Packet
        if (stateManager.toggleMode(this.getBurnTime())) {
            this.setChanged();
            this.syncToClient();
        }
    }

    @RPC(side = Side.SERVER)
    public void openUpgradeGui(Player player) {
        // 帶參數的 RPC
        player.openMenu(new UpgradeMenuProvider(this));
    }
}
```

**Screen 端調用**：
```java
public class ManaGeneratorScreen extends ModularScreen<ManaGeneratorMenu> {

    @Override
    protected void buildGui(Panel root) {
        root.add(new ButtonWidget(130, 25, 20, 20, BUTTON_TEXTURE, btn -> {
            // 簡化！不需要手寫 Packet
            RPC.call(menu.getBlockEntityPos(), "toggleMode");
        }));

        root.add(new ButtonWidget(150, 5, 18, 18, UPGRADE_TEXTURE, btn -> {
            RPC.call(menu.getBlockEntityPos(), "openUpgradeGui", minecraft.player);
        }));
    }
}
```

---

### 3. 效能優化

#### **Dirty 檢測（已實現，需強化）**

```java
public class OptimizedSyncManager {

    // 追蹤變化的字段
    private final Set<String> dirtyFields = new HashSet<>();

    // 批量同步間隔
    private int syncInterval = 5;  // 每 5 tick 同步一次
    private int tickCounter = 0;

    public void tick() {
        tickCounter++;
        if (tickCounter >= syncInterval && !dirtyFields.isEmpty()) {
            // 只同步變化的字段
            syncDirtyFields();
            dirtyFields.clear();
            tickCounter = 0;
        }
    }

    // 差分更新：只發送變化的數據
    private void syncDirtyFields() {
        DeltaSyncPacket packet = new DeltaSyncPacket();
        for (String field : dirtyFields) {
            packet.addField(field, getFieldValue(field));
        }
        sendToClients(packet);
    }
}
```

#### **智能同步策略**

```java
public @interface BindField {
    // 同步策略
    SyncStrategy strategy() default SyncStrategy.ON_CHANGE;
}

public enum SyncStrategy {
    ALWAYS,        // 每次都同步
    ON_CHANGE,     // 值改變時同步（預設）
    THRESHOLD,     // 變化超過閾值才同步（用於數值）
    INTERVAL       // 固定間隔同步
}
```

---

### 4. 自動 Widget 註冊

#### **新增註解**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoWidget {
    /** Widget 類型 */
    Class<? extends AbstractWidget> type();

    /** 位置 */
    int x();
    int y();

    /** 尺寸（可選） */
    int width() default -1;
    int height() default -1;
}
```

#### **使用範例**

```java
public class ManaGeneratorScreen extends ModularScreen<ManaGeneratorMenu> {

    @AutoWidget(type = ManaBarWidget.class, x = 11, y = 19)
    private ManaBarWidget manaBar;

    @AutoWidget(type = EnergyBarWidget.class, x = 156, y = 19)
    private EnergyBarWidget energyBar;

    @Override
    protected void buildGui(Panel root) {
        // 自動掃描並註冊所有 @AutoWidget
        WidgetRegistry.autoRegister(this, root);

        // 或手動添加其他 Widget
        root.add(new ButtonWidget(...));
    }
}
```

---

## 🔧 實現計劃

### **階段 1：數據綁定核心**（優先）
1. ✅ 已有：`@Sync` 註解 + `MachineSyncManager`
2. 🆕 新增：`@BindField` 註解
3. 🆕 新增：`BindingManager` - 自動掃描並綁定
4. 🆕 新增：雙向綁定邏輯

### **階段 2：RPC 系統**（中等優先）
1. 🆕 新增：`@RPC` 註解
2. 🆕 新增：`RPCManager` - 自動生成 Packet
3. 🆕 新增：`RPC.call()` 工具方法

### **階段 3：效能優化**（持續）
1. ✅ 已有：Dirty 檢測基礎
2. 🆕 強化：差分同步
3. 🆕 新增：智能同步策略
4. 🆕 新增：批量更新

### **階段 4：自動註冊**（可選）
1. 🆕 新增：`@AutoWidget` 註解
2. 🆕 新增：`WidgetRegistry` - 反射掃描

---

## 📝 授權聲明

本設計方案完全原創，基於以下技術：
- Java 反射 API（標準庫）
- NeoForge 網絡系統（官方 API）
- 自研的 Widget 架構（MIT 授權）

**不包含任何 LDLib2（GPL）的代碼**。

---

## 🎯 與 LDLib2 的對比

| 功能 | LDLib2 (GPL) | 我們的方案 (MIT) |
|------|-------------|-----------------|
| 數據綁定 | ✅ | ✅ @BindField |
| RPC 通信 | ✅ | ✅ @RPC |
| 效能優化 | ✅ | ✅ 差分同步 |
| XML UI | ✅ | ❌ 不需要 |
| 視覺編輯器 | ✅ | ❌ 不實現 |
| Widget 系統 | ✅ | ✅ 已有 |
| 註解驅動 | ✅ | ✅ 完全支援 |

---

## 🚀 下一步

1. **先測試當前遊戲** - 確認基礎功能正常
2. **決定實現順序** - 你最想先實現哪個功能？
3. **逐步增強** - 從最有價值的功能開始

建議先實現：**階段 1（數據綁定）** + **階段 2（RPC 系統）**
