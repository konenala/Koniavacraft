package com.github.nalamodikk.common.rpg;

import com.github.nalamodikk.common.rpg.data.PlayerRPGData;
import com.github.nalamodikk.common.rpg.skill.PlayerSkillData;
import com.github.nalamodikk.common.rpg.skill.SkillRegistry;
import com.github.nalamodikk.register.ModDataAttachments;
import net.minecraft.world.entity.player.Player;

/**
 * 🎮 RPG 系統管理器
 *
 * 統一管理所有 RPG 相關功能:
 * - 玩家數據訪問
 * - 技能數據訪問
 * - 經驗/等級處理
 * - 屬性計算
 */
public class RPGManager {

    /**
     * 🎯 獲取玩家 RPG 數據
     *
     * 使用 NeoForge Attachment API 訪問玩家數據
     * 數據會自動持久化到玩家 NBT
     */
    public static PlayerRPGData getPlayerData(Player player) {
        return player.getData(ModDataAttachments.PLAYER_RPG_DATA);
    }

    /**
     * 🎯 獲取玩家技能數據
     *
     * 使用 NeoForge Attachment API 訪問技能數據
     * 數據會自動持久化到玩家 NBT
     */
    public static PlayerSkillData getSkillData(Player player) {
        return player.getData(ModDataAttachments.PLAYER_SKILL_DATA);
    }

    /**
     * ⭐ 給予玩家經驗值
     *
     * @param player 玩家
     * @param amount 經驗值數量
     * @return 是否升級
     */
    public static boolean giveExperience(Player player, int amount) {
        PlayerRPGData data = getPlayerData(player);
        if (data == null) return false;

        boolean leveledUp = data.addExperience(amount);

        if (leveledUp) {
            onPlayerLevelUp(player, data);
        }

        // TODO: 同步到客戶端
        return leveledUp;
    }

    /**
     * 📈 升級回調處理
     */
    private static void onPlayerLevelUp(Player player, PlayerRPGData data) {
        // TODO: 播放升級音效
        // TODO: 顯示升級粒子效果
        // TODO: 發送升級訊息到聊天框
        // TODO: 觸發升級事件
    }

    /**
     * 🔄 分配屬性點
     *
     * @param player 玩家
     * @param attributeName 屬性名稱
     * @param amount 點數
     * @return 是否成功
     */
    public static boolean allocateAttribute(Player player, String attributeName, int amount) {
        PlayerRPGData data = getPlayerData(player);
        if (data == null) return false;

        boolean success = data.allocateAttributePoint(attributeName, amount);

        if (success) {
            // TODO: 同步到客戶端
            // TODO: 更新玩家屬性 (生命值、魔力等)
        }

        return success;
    }

    /**
     * ⚔️ 計算玩家近戰傷害
     *
     * @param player 玩家
     * @param baseDamage 基礎傷害
     * @return 實際傷害
     */
    public static float calculateMeleeDamage(Player player, float baseDamage) {
        PlayerRPGData data = getPlayerData(player);
        if (data == null) return baseDamage;

        float multiplier = data.getAttributes().getMeleeDamageMultiplier();
        return baseDamage * multiplier;
    }

    /**
     * 🔮 計算玩家魔法傷害
     */
    public static float calculateMagicDamage(Player player, float baseDamage) {
        PlayerRPGData data = getPlayerData(player);
        if (data == null) return baseDamage;

        float multiplier = data.getAttributes().getMagicDamageMultiplier();
        return baseDamage * multiplier;
    }

    /**
     * 🕐 計算技能實際冷卻時間
     *
     * @param player 玩家
     * @param baseCooldown 基礎冷卻 (ticks)
     * @return 實際冷卻 (ticks)
     */
    public static int calculateSkillCooldown(Player player, int baseCooldown) {
        PlayerRPGData data = getPlayerData(player);
        if (data == null) return baseCooldown;

        return data.getAttributes().calculateSkillCooldown(baseCooldown);
    }

    /**
     * 🚀 初始化 RPG 系統
     * 在模組載入時調用
     */
    public static void init() {
        // 初始化技能註冊表
        SkillRegistry.init();

        // TODO: 註冊事件監聽器
        // TODO: 註冊網路封包
    }
}
