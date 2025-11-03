package com.github.nalamodikk.common.rpg.skill;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * ⚔️ 技能抽象基類
 *
 * 所有技能都繼承此類
 */
public abstract class Skill {

    private final String id;
    private final String name;
    private final int baseCooldown; // ticks
    private final int manaCost;
    private final SkillType type;

    public Skill(String id, String name, int baseCooldown, int manaCost, SkillType type) {
        this.id = id;
        this.name = name;
        this.baseCooldown = baseCooldown;
        this.manaCost = manaCost;
        this.type = type;
    }

    // ===== Getter =====

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getBaseCooldown() {
        return baseCooldown;
    }

    public int getManaCost() {
        return manaCost;
    }

    public SkillType getType() {
        return type;
    }

    // ===== 抽象方法 =====

    /**
     * 🎯 施放技能
     * @param player 施放者
     * @param level 世界
     * @return 是否成功施放
     */
    public abstract boolean cast(Player player, Level level);

    /**
     * 📋 獲取技能描述
     */
    public abstract String getDescription();

    /**
     * ✅ 檢查是否可以施放
     * @param player 施放者
     * @return 是否可施放
     */
    public boolean canCast(Player player) {
        // TODO: 檢查魔力、冷卻、職業等
        return true;
    }

    /**
     * 🎨 獲取技能圖標路徑
     */
    public String getIconPath() {
        return "koniava:textures/skill/" + id + ".png";
    }
}
