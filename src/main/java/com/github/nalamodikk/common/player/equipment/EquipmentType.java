package com.github.nalamodikk.common.player.equipment;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

// EquipmentType.java
public enum EquipmentType {
    // 使用原版欄位的裝備
    HELMET("equipment.koniava.helmet", true, -1, -1),
    CHESTPLATE("equipment.koniava.chestplate", true, -1, -1),
    LEGGINGS("equipment.koniava.leggings", true, -1, -1),
    BOOTS("equipment.koniava.boots", true, -1, -1),

    // 新增的額外裝備 (u, v 座標對應圖片中的位置)
    SHOULDER_PAD("equipment.koniava.shoulder_pad", false, 0, 0),    // 第1個圖示
    ARM_ARMOR("equipment.koniava.arm_armor", false, 16, 0),        // 第2個圖示
    BELT("equipment.koniava.belt", false, 32, 0),                  // 第3個圖示
    GLOVES("equipment.koniava.gloves", false, 48, 0),              // 第4個圖示
    GOGGLES("equipment.koniava.goggles", false, 64, 0),            // 第5個圖示
    ENGINE("equipment.koniava.engine", false, 80, 0),              // 第6個圖示
    REACTOR("equipment.koniava.reactor", false, 96, 0),            // 第7個圖示
    EXOSKELETON("equipment.koniava.exoskeleton", false, 112, 0);   // 第8個圖示


    private final String translationKey;
    private final boolean isVanillaSlot;
    private final int iconU;  // 圖示在紋理中的 U 座標
    private final int iconV;  // 圖示在紋理中的 V 座標
    public int getIconU() { return iconU; }
    public int getIconV() { return iconV; }

    EquipmentType(String translationKey, boolean isVanillaSlot, int iconU, int iconV) {
        this.translationKey = translationKey;
        this.isVanillaSlot = isVanillaSlot;
        this.iconU = iconU;
        this.iconV = iconV;
    }
    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isVanillaSlot() {
        return isVanillaSlot;
    }

    // 獲取本地化的顯示名稱
    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    // 獲取原始字串（用於調試或fallback）
    public String getRawDisplayName() {
        return translationKey;
    }

    public static ResourceLocation getSlotIconTexture() {
        return ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "equipment_slots");
    }
}