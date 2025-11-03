package com.github.nalamodikk.common.rpg.player;

/**
 * 🎭 玩家職業枚舉
 *
 * 三大職業:
 * - 戰士 (Warrior): 近戰物理輸出
 * - 法師 (Mage): 遠程魔法輸出
 * - 遊俠 (Ranger): 遠程物理輸出
 */
public enum PlayerClass {
    NONE("none"),
    WARRIOR("warrior"),
    MAGE("mage"),
    RANGER("ranger");

    private final String id;

    PlayerClass(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * 獲取翻譯鍵
     */
    public String getTranslationKey() {
        return "rpg.class." + id;
    }

    /**
     * 從 ID 獲取職業
     */
    public static PlayerClass fromId(String id) {
        for (PlayerClass clazz : values()) {
            if (clazz.id.equals(id)) {
                return clazz;
            }
        }
        return NONE;
    }

    /**
     * 📋 獲取職業描述翻譯鍵
     */
    public String getDescriptionKey() {
        return "rpg.class." + id + ".description";
    }

    /**
     * 🎯 獲取職業推薦主屬性翻譯鍵
     */
    public String getPrimaryAttributesKey() {
        return "rpg.class." + id + ".attributes";
    }
}
