// 已重構 BasicTechWandItem：使用 NeoForge DataComponent API + Packet + 滾輪切換 + 儲存方向提示
package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.network.packet.manatool.TechWandModePacket;
import com.github.nalamodikk.common.register.ModDataComponents;
import com.github.nalamodikk.common.utils.data.TechDataComponents;
import com.github.nalamodikk.common.utils.helpers.BlockSelector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class BasicTechWandItem extends Item {

    public BasicTechWandItem(Properties properties) {
        super(properties.component(ModDataComponents.TECH_WAND_MODE, TechWandMode.CONFIGURE_IO));
        NeoForge.EVENT_BUS.addListener(this::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onMouseScroll);
    }

    public TechWandMode getMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TECH_WAND_MODE, TechWandMode.CONFIGURE_IO);
    }

    public void setMode(ItemStack stack, TechWandMode mode) {
        stack.set(ModDataComponents.TECH_WAND_MODE, mode);
    }

    public void changeMode(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            TechWandMode current = getMode(stack);
            TechWandMode next = current.next();
            setMode(stack, next);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.magical_industry.mode_changed",
                    Component.translatable("mode.magical_industry." + next.getSerializedName())
            ), true);
        }
    }

    public void onLeftClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.isCrouching()) return;

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof BasicTechWandItem)) return;

        Level level = player.level();
        if (level.isClientSide) return;

        BlockPos target = BlockSelector.getTargetBlock(player, 5.0);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.magical_industry.no_block_selected"), true);
            return;
        }

        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof IConfigurableBlock configBlock) {
            TechDataComponents.saveConfigDirections(stack, target, configBlock);
            player.displayClientMessage(Component.translatable("message.magical_industry.block_selected", target), true);
            event.setCanceled(true);
        } else {
            player.displayClientMessage(Component.translatable("message.magical_industry.block_not_configurable"), true);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isCrouching()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        BlockEntity be = level.getBlockEntity(pos);

        if (!level.isClientSide && be instanceof IConfigurableBlock configBlock) {
            TechWandMode mode = getMode(stack);
            Direction face = context.getClickedFace();

            switch (mode) {
                case DIRECTION_CONFIG -> {
                    boolean output = configBlock.isOutput(face);
                    configBlock.setDirectionConfig(face, !output);
                    player.displayClientMessage(Component.translatable(
                            "message.magical_industry.config_changed",
                            face.getName(),
                            !output ? Component.translatable("mode.magical_industry.output") : Component.translatable("mode.magical_industry.input")
                    ), true);
                    return InteractionResult.SUCCESS;
                }
                case CONFIGURE_IO -> {
                    if (player instanceof ServerPlayer sp) {
                        NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                                (id, inv, p) -> new UniversalConfigMenu(id, inv, be, stack),
                                Component.translatable("screen.magical_industry.configure_io")
                        ), buf -> {
                            buf.writeBlockPos(be.getBlockPos());
                            buf.writeItem(stack);
                        });
                    }
                    return InteractionResult.SUCCESS;
                }

                case ROTATE -> {
                    BlockState state = level.getBlockState(pos);
                    if (state.hasProperty(BlockStateProperties.FACING)) {
                        level.setBlock(pos, state.setValue(BlockStateProperties.FACING,
                                state.getValue(BlockStateProperties.FACING).getClockWise()), 3);
                        player.displayClientMessage(Component.translatable("message.magical_industry.block_rotated_facing"), true);
                        return InteractionResult.SUCCESS;
                    } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        level.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                                state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise()), 3);
                        player.displayClientMessage(Component.translatable("message.magical_industry.block_rotated_horizontal"), true);
                        return InteractionResult.SUCCESS;
                    } else {
                        player.displayClientMessage(Component.translatable("message.magical_industry.block_cannot_rotate"), true);
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }


    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isCrouching()) return;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof BasicTechWandItem wand)) return;

        boolean forward = event.getScrollDeltaY() > 0;
        TechWandMode current = wand.getMode(held);
        TechWandMode next = current.cycle(forward); // ⬅ 你需要這個方法
        TechWandModePacket.sendToServer(next);

        MagicalIndustryMod.LOGGER.debug("Sending TechWandModePacket: " + next);
        event.setCanceled(true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.magical_industry.mode",
                Component.translatable("mode.magical_industry." + getMode(stack).getSerializedName())));
        TechDataComponents.appendSavedDirectionTooltip(stack, tooltipComponents);
    }
}
