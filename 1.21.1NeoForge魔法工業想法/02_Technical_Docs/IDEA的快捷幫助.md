# IDEA 查資料神器快捷鍵

## 🚀 **最重要的快捷鍵**

### **Ctrl + Space** - 自動完成
```java
vertexConsumer.add[Ctrl+Space]
// 會顯示所有可用的方法：addVertex(), ...
```

### **Ctrl + Shift + Space** - 智能完成
```java
renderType.create("name", [Ctrl+Shift+Space]
// 會建議符合類型的參數
```

### **Ctrl + P** - 參數提示
```java
vertexConsumer.addVertex([Ctrl+P]
// 顯示：addVertex(Matrix4f pose, float x, float y, float z)
```

### **Ctrl + Q** - 快速文檔
```java
// 游標放在方法名上按 Ctrl+Q
vertexConsumer.setColor() // 會顯示方法說明
```

### **Ctrl + Click** - 進入定義
```java
// Ctrl+點擊任何方法/類，直接看原始碼
VertexConsumer // 點擊就能看到接口定義
```

### **Ctrl + Alt + B** - 查看實現
```java
// 查看接口的所有實現類
VertexConsumer // 能看到具體是怎麼實現的
```

## 🔍 **搜尋相關功能**

### **Ctrl + Shift + F** - 全局搜尋
```
搜尋：setColor
// 找到項目中所有使用 setColor 的地方
```

### **Ctrl + Alt + F7** - 查看使用位置
```java
// 游標放在方法上，看哪裡用了這個方法
setColor() // 能看到所有調用的地方
```

### **Double Shift** - 搜尋所有東西
```
搜尋：RenderType
// 能找到類、檔案、設定等所有相關內容
```

## 📖 **學習他人代碼的方法**

### **F4** - 查看源碼
```java
// 想知道 Minecraft 怎麼渲染方塊？
Block.class // 按 F4 直接看原始碼
```

### **Alt + F7** - 找使用範例
```java
// 想知道某個方法怎麼用？
addVertex() // 看看 Minecraft 在哪裡用了這個方法
```