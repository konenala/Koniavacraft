package com.github.nalamodikk.common.item.ritual;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 共鳴水晶 - 啟動儀式的核心催化劑
 * 消耗品，每次儀式都需要消耗一個
 */
public class ResonantCrystalItem extends Item {

    public ResonantCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.resonant_crystal.category"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.resonant_crystal.description"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.resonant_crystal.usage"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.resonant_crystal.expendable"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // 始終顯示附魔光效
    }

    /**
     * 檢查此水晶是否可以用於特定類型的儀式
     */
    public boolean canCatalyzeMask(RitualTier tier) {
        // 共鳴水晶可以催化除禁忌儀式外的所有儀式
        return tier != RitualTier.FORBIDDEN;
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
