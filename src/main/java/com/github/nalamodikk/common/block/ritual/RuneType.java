package com.github.nalamodikk.common.block.ritual;

/**
 * 符文石類型枚舉
 * 定義了四種基礎符文石的類型和效果
 */
public enum RuneType {
    /**
     * 效率符文 - 降低儀式魔力消耗
     * 每塊符文石使儀式總魔力消耗降低 8%，最多疊加 5 塊 (40%)
     */
    EFFICIENCY("efficiency", 0xFF3366BB, 0.08f, 5),

    /**
     * 迅捷符文 - 提升儀式速度
     * 每塊符文石使儀式速度提升 10%，最多疊加 5 塊 (50%)
     */
    CELERITY("celerity", 0xFF66BB33, 0.10f, 5),

    /**
     * 穩定符文 - 降低儀式失敗機率和負面效果
     * 每塊符文石顯著降低儀式失敗時的負面效果
     */
    STABILITY("stability", 0xFFBB6633, 0.15f, 8),

    /**
     * 增幅符文 - 增加產物數量或品質的機率
     * 有機率使產物數量翻倍或附加良性效果
     */
    AUGMENTATION("augmentation", 0xFFBB3366, 0.12f, 6);

    private final String name;
    private final int glowColor;
    private final float effectValue;
    private final int maxStack;

    RuneType(String name, int glowColor, float effectValue, int maxStack) {
        this.name = name;
        this.glowColor = glowColor;
        this.effectValue = effectValue;
        this.maxStack = maxStack;
    }

    public String getName() {
        return name;
    }

    public int getGlowColor() {
        return glowColor;
    }

    public float getEffectValue() {
        return effectValue;
    }

    public int getMaxStack() {
        return maxStack;
    }

    /**
     * 計算此類型符文石的實際效果值
     *
     * @param count 符文石數量
     * @return 實際效果值（已考慮堆疊上限）
     */
    public float calculateEffect(int count) {
        int actualCount = Math.min(count, maxStack);
        return effectValue * actualCount;
    }

    /**
     * 檢查是否達到最大堆疊數
     *
     * @param count 符文石數量
     * @return 是否達到最大堆疊數
     */
    public boolean isMaxed(int count) {
        return count >= maxStack;
    }

    /**
     * 獲取翻譯鍵
     *
     * @return 方塊翻譯鍵
     */
    public String getTranslationKey() {
        return "block.koniavacraft.rune_stone_" + name;
    }

    /**
     * 獲取描述翻譯鍵
     *
     * @return 工具提示翻譯鍵
     */
    public String getDescriptionKey() {
        return "tooltip.koniavacraft.rune_stone_" + name + ".description";
    }
}