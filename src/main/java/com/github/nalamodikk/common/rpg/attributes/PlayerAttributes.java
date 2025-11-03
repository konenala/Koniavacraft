package com.github.nalamodikk.common.rpg.attributes;

/**
 * 🎮 玩家 RPG 屬性系統
 *
 * 五大核心屬性:
 * - 力量 (Strength): 影響近戰傷害
 * - 智力 (Intelligence): 影響魔法傷害和魔力上限
 * - 敏捷 (Agility): 影響攻擊速度和閃避
 * - 體質 (Vitality): 影響生命值和傷害減免
 * - 感知 (Perception): 影響技能冷卻時間 (CDR)
 */
public class PlayerAttributes {

    // ===== 基礎屬性 =====
    private int strength = 0;      // 力量
    private int intelligence = 0;  // 智力
    private int agility = 0;       // 敏捷
    private int vitality = 0;      // 體質
    private int perception = 0;    // 感知

    // ===== 屬性效果常數 =====

    // 力量效果
    private static final float STRENGTH_MELEE_DAMAGE_PER_POINT = 0.02f; // 2% per point

    // 智力效果
    private static final float INTELLIGENCE_MAGIC_DAMAGE_PER_POINT = 0.02f; // 2% per point
    private static final int INTELLIGENCE_MANA_PER_POINT = 10; // 10 mana per point

    // 敏捷效果
    private static final float AGILITY_ATTACK_SPEED_PER_POINT = 0.01f; // 1% per point
    private static final float AGILITY_DODGE_PER_POINT = 0.005f; // 0.5% per point

    // 體質效果
    private static final int VITALITY_HEALTH_PER_POINT = 2; // 2 HP per point
    private static final float VITALITY_DAMAGE_REDUCTION_PER_POINT = 0.005f; // 0.5% per point

    // 感知效果
    private static final float PERCEPTION_CDR_PER_POINT = 0.0025f; // 0.25% per point
    private static final int PERCEPTION_MAX_POINTS = 200; // 上限 200 點
    private static final float PERCEPTION_MAX_CDR = 0.50f; // 最大 50% 冷卻縮減

    // ===== Getter/Setter =====

    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = Math.max(0, strength); }

    public int getIntelligence() { return intelligence; }
    public void setIntelligence(int intelligence) { this.intelligence = Math.max(0, intelligence); }

    public int getAgility() { return agility; }
    public void setAgility(int agility) { this.agility = Math.max(0, agility); }

    public int getVitality() { return vitality; }
    public void setVitality(int vitality) { this.vitality = Math.max(0, vitality); }

    public int getPerception() { return perception; }
    public void setPerception(int perception) { this.perception = Math.max(0, perception); }

    // ===== 計算方法 =====

    /**
     * 📊 計算近戰傷害加成
     * @return 傷害倍率 (1.0 = 100%)
     */
    public float getMeleeDamageMultiplier() {
        return 1.0f + (strength * STRENGTH_MELEE_DAMAGE_PER_POINT);
    }

    /**
     * 🔮 計算魔法傷害加成
     * @return 傷害倍率 (1.0 = 100%)
     */
    public float getMagicDamageMultiplier() {
        return 1.0f + (intelligence * INTELLIGENCE_MAGIC_DAMAGE_PER_POINT);
    }

    /**
     * 💙 計算最大魔力值加成
     * @param baseMana 基礎魔力值
     * @return 實際最大魔力值
     */
    public int getMaxMana(int baseMana) {
        return baseMana + (intelligence * INTELLIGENCE_MANA_PER_POINT);
    }

    /**
     * ⚡ 計算攻擊速度加成
     * @return 速度倍率 (1.0 = 100%)
     */
    public float getAttackSpeedMultiplier() {
        return 1.0f + (agility * AGILITY_ATTACK_SPEED_PER_POINT);
    }

    /**
     * 🌀 計算閃避率
     * @return 閃避率 (0.0 - 1.0)
     */
    public float getDodgeChance() {
        return Math.min(agility * AGILITY_DODGE_PER_POINT, 0.75f); // 上限 75%
    }

    /**
     * ❤️ 計算最大生命值加成
     * @param baseHealth 基礎生命值
     * @return 實際最大生命值
     */
    public int getMaxHealth(int baseHealth) {
        return baseHealth + (vitality * VITALITY_HEALTH_PER_POINT);
    }

    /**
     * 🛡️ 計算傷害減免
     * @return 減免率 (0.0 - 1.0)
     */
    public float getDamageReduction() {
        return Math.min(vitality * VITALITY_DAMAGE_REDUCTION_PER_POINT, 0.75f); // 上限 75%
    }

    /**
     * 🕐 計算技能冷卻時間
     * @param baseCooldown 基礎冷卻時間 (ticks)
     * @return 實際冷卻時間 (ticks)
     */
    public int calculateSkillCooldown(int baseCooldown) {
        // 限制感知點數上限為 200
        int effectivePerception = Math.min(perception, PERCEPTION_MAX_POINTS);

        // 計算冷卻縮減率
        float cooldownReduction = effectivePerception * PERCEPTION_CDR_PER_POINT;
        cooldownReduction = Math.min(cooldownReduction, PERCEPTION_MAX_CDR); // 確保不超過 50%

        // 計算實際冷卻時間
        int actualCooldown = (int) (baseCooldown * (1.0f - cooldownReduction));

        return Math.max(actualCooldown, 1); // 最少 1 tick
    }

    /**
     * 📈 獲取當前冷卻縮減百分比 (用於 UI 顯示)
     * @return CDR 百分比 (0.0 - 50.0)
     */
    public float getCooldownReductionPercent() {
        int effectivePerception = Math.min(perception, PERCEPTION_MAX_POINTS);
        float cdr = effectivePerception * PERCEPTION_CDR_PER_POINT * 100;
        return Math.min(cdr, PERCEPTION_MAX_CDR * 100);
    }

    /**
     * ℹ️ 檢查感知屬性是否已達上限
     * @return true 如果已達 200 點上限
     */
    public boolean isPerceptionCapped() {
        return perception >= PERCEPTION_MAX_POINTS;
    }

    /**
     * 🔄 增加屬性點
     */
    public void addStrength(int amount) { this.strength += amount; }
    public void addIntelligence(int amount) { this.intelligence += amount; }
    public void addAgility(int amount) { this.agility += amount; }
    public void addVitality(int amount) { this.vitality += amount; }
    public void addPerception(int amount) { this.perception += amount; }

    /**
     * 📊 獲取總屬性點數 (用於驗證)
     */
    public int getTotalAttributePoints() {
        return strength + intelligence + agility + vitality + perception;
    }
}
