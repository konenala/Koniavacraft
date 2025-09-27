package com.github.nalamodikk.common.item.ritual;

import com.github.nalamodikk.common.block.ritualblock.ChalkGlyphBlock;
import com.github.nalamodikk.register.ModBlocks;
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
 * 儀式師的粉筆 - 可放置的粉筆符號方塊
 * 不同顏色的粉筆用於不同類型的儀式驗證
 */
public class RitualistChalkItem extends Item {
    private static final int MAX_DAMAGE = 64; // 可使用64次
    private final ChalkGlyphBlock.ChalkColor chalkColor;

    public RitualistChalkItem(Properties properties, ChalkGlyphBlock.ChalkColor color) {
        super(properties.durability(MAX_DAMAGE));
        this.chalkColor = color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Direction face = context.getClickedFace();

        if (player == null || face != Direction.UP) {
            return InteractionResult.PASS;
        }

        BlockPos placePos = clickedPos.above();

        // 檢查是否可以放置
        if (!canPlaceGlyph(level, clickedPos, placePos)) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.koniavacraft.chalk.cannot_place"));
            }
            return InteractionResult.FAIL;
        }

        // 檢查目標位置是否已有粉筆符號
        BlockState targetState = level.getBlockState(placePos);
        if (targetState.getBlock() instanceof ChalkGlyphBlock) {
            // 如果是同色粉筆，切換圖案（在方塊的 useWithoutItem 中處理）
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            // 放置粉筆符號方塊
            BlockState glyphState = ModBlocks.CHALK_GLYPH.get().defaultBlockState()
                    .setValue(ChalkGlyphBlock.COLOR, chalkColor)
                    .setValue(ChalkGlyphBlock.PATTERN, ChalkGlyphBlock.GlyphPattern.CIRCLE);

            if (level.setBlock(placePos, glyphState, 3)) {
                // 消耗耐久度
                stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));

                player.sendSystemMessage(Component.translatable("message.koniavacraft.chalk.placed", chalkColor.getDisplayComponent()));
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 檢查是否可以放置粉筆符號
     */
    private boolean canPlaceGlyph(Level level, BlockPos supportPos, BlockPos placePos) {
        // 檢查支撐方塊是否合適
        BlockState supportBlock = level.getBlockState(supportPos);
        if (!isValidSurface(supportBlock)) {
            return false;
        }

        // 檢查放置位置是否可替換（空氣或可覆蓋的方塊）
        BlockState placeBlockState = level.getBlockState(placePos);
        return placeBlockState.isAir() || placeBlockState.canBeReplaced();
    }

    /**
     * 檢查是否為有效的支撐表面
     */
    private boolean isValidSurface(BlockState state) {
        // 允許在大多數固體方塊上繪製
        return state.isFaceSturdy(null, null, Direction.UP) &&
               !state.is(Blocks.WATER) &&
               !state.is(Blocks.LAVA);
    }

    public ChalkGlyphBlock.ChalkColor getChalkColor() {
        return chalkColor;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        int damage = stack.getDamageValue();
        int remaining = MAX_DAMAGE - damage;

        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.chalk.color", chalkColor.getDisplayComponent()));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.chalk.uses", remaining, MAX_DAMAGE));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.chalk.usage"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.chalk.switch_pattern"));
        tooltipComponents.add(Component.translatable("tooltip.koniavacraft.chalk.purpose"));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return switch (chalkColor) {
            case WHITE -> 0xFFFFFF;
            case YELLOW -> 0xFFFF00;
            case BLUE -> 0x0080FF;
            case PURPLE -> 0x9400D3;
            case RED -> 0xFF0000;
            case GREEN -> 0x00FF00;
        };
    }
}
