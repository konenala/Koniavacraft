package com.github.nalamodikk.common.item.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 儀式師的粉筆 - 用於在地面上繪製魔法陣圖案
 * 定義儀式區域和類型，有耐久度限制
 */
public class RitualistChalkItem extends Item {
    private static final int MAX_DAMAGE = 64; // 可使用64次
    
    public RitualistChalkItem(Properties properties) {
        super(properties.durability(MAX_DAMAGE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Direction face = context.getClickedFace();

        if (player == null || face != Direction.UP) {
            return InteractionResult.PASS;
        }

        // 檢查是否在平坦的表面上使用
        BlockState clickedBlock = level.getBlockState(pos);
        if (!isValidSurface(clickedBlock)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("需要在平坦的石質表面上繪製魔法陣"));
            }
            return InteractionResult.FAIL;
        }

        // 檢查上方是否為空氣
        BlockPos abovePos = pos.above();
        if (!level.getBlockState(abovePos).isAir()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("上方空間不足以繪製魔法陣"));
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            // 這裡將來會實現魔法陣繪製邏輯
            // 目前先顯示提示信息
            player.sendSystemMessage(Component.literal("在此處繪製了魔法陣基礎結構"));
            
            // 消耗耐久度
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 檢查是否為有效的繪製表面
     */
    private boolean isValidSurface(BlockState state) {
        // 允許在石質方塊上繪製
        return state.is(Blocks.STONE) ||
               state.is(Blocks.COBBLESTONE) ||
               state.is(Blocks.STONE_BRICKS) ||
               state.is(Blocks.POLISHED_BLACKSTONE) ||
               state.is(Blocks.BLACKSTONE) ||
               state.is(Blocks.DEEPSLATE) ||
               state.is(Blocks.POLISHED_DEEPSLATE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        int damage = stack.getDamageValue();
        int remaining = MAX_DAMAGE - damage;
        
        tooltipComponents.add(Component.literal("剩余使用次數: " + remaining + "/" + MAX_DAMAGE));
        tooltipComponents.add(Component.literal("右鍵點擊石質表面繪製魔法陣"));
        tooltipComponents.add(Component.literal("用於定義儀式區域和類型"));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x9400D3; // 紫色耐久度條
    }
}