package com.github.nalamodikk.common.item.ritual;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 虛空珍珠 - 用於高級和禁忌儀式的催化劑
 * 可能導致不穩定事件，具有危險性
 */
public class VoidPearlItem extends Item {
    
    public VoidPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.void_pearl.category"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.void_pearl.description"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.void_pearl.usage"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.void_pearl.warning"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.void_pearl.expendable"));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        
        // 虛空珍珠在物品欄中會產生微弱的不穩定效應
        if (entity instanceof Player player && level.getGameTime() % 200 == 0) { // 每10秒
            if (level.random.nextFloat() < 0.05f) { // 5%機率
                // 給予玩家短暫的虛弱效果作為持有懲罰
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.translatable("message.koniavacraft.void_pearl.ominous_energy"));
                }
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // 始終顯示附魔光效
    }

    /**
     * 檢查此珍珠是否可以用於特定類型的儀式
     */
    public boolean canCatalyzeMask(RitualTier tier) {
        // 虛空珍珠可以催化所有類型的儀式，包括禁忌儀式
        return true;
    }

    /**
     * 獲取使用此催化劑時的不穩定性加成
     */
    public float getInstabilityModifier() {
        return 0.3f; // 增加30%的不穩定性
    }

    /**
     * 儀式等級枚舉
     */
    public enum RitualTier {
        BASIC,      // 基礎儀式
        ADVANCED,   // 高級儀式
        MASTER,     // 大師級儀式
        FORBIDDEN   // 禁忌儀式
    }
}