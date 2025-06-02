// 已重構 BasicTechWandItem：使用 NeoForge DataComponent API + Packet + 滾輪切換 + 儲存方向提示
/**
 * 🔧 BasicTechWandItem
 *
 * 本類是自訂的科技魔杖物品，繼承自 Minecraft 的 Item 類別。
 * 其邏輯透過 `useOn()` 方法在玩家右鍵方塊時自動觸發，
 * 無需事件訂閱（不需使用 @SubscribeEvent 或 EventBus）。
 *
 * 此物品可搭配 TechWandMode 進行模式切換操作，
 * 如：輸入輸出配置、方向設定、自轉行為等。
 *
 * ✅ 此類需註冊至 ModItems，讓遊戲認得它是一個有效物品。
 */

package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.API.IConfigurableBlock;
import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ManaUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.TechWandModePacket;
import com.github.nalamodikk.common.register.ModDataComponents;
import com.github.nalamodikk.common.screen.shared.UniversalConfigMenu;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.data.CodecsLibrary;
import com.github.nalamodikk.common.utils.data.TechDataComponents;
import com.github.nalamodikk.common.utils.block.BlockSelectorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

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

        BlockPos target = BlockSelectorUtils.getTargetBlock(player, 5.0);
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
                    IOHandlerUtils.IOType current = configBlock.getIOConfig(face);
                    IOHandlerUtils.IOType next = IOHandlerUtils.nextIOType(current);
                    configBlock.setIOConfig(face, next);

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity != null) {
                        PacketDistributor.sendToPlayer((ServerPlayer) player,
                                new ConfigDirectionUpdatePacket(blockEntity.getBlockPos(), face, next)); // ✅ 改為傳 IOType
                    }

                    player.displayClientMessage(Component.translatable(
                            "message.magical_industry.config_changed",
                            face.getName(),
                            Component.translatable("mode.magical_industry." + next.name().toLowerCase()) // ✅ 改為使用 next.name()
                    ), true);

                    return InteractionResult.SUCCESS;
                }

                case CONFIGURE_IO -> {
                    if (player instanceof ServerPlayer sp) {
                        IConfigurableBlock configurableBlock = (IConfigurableBlock) be;
                        var manaStorage = CapabilityUtils.getMana(sp.level(), be.getBlockPos(), null);
                        if (manaStorage != null) {
                            ManaUpdatePacket.sendManaUpdate(sp, be.getBlockPos(), manaStorage.getManaStored());
                        }

                        sp.openMenu(new MenuProvider() {
                            @Override
                            public Component getDisplayName() {
                                return Component.translatable("screen.magical_industry.configure_io");
                            }

                            @Override
                            public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
                                return new UniversalConfigMenu(id, inv, be, stack); // ✅ 伺服器端建構用
                            }
                        }, (buf) -> {
                            buf.writeBlockPos(be.getBlockPos());
                            buf.writeWithCodec(NbtOps.INSTANCE, ItemStack.CODEC, stack);
                            buf.writeWithCodec(NbtOps.INSTANCE, CodecsLibrary.DIRECTION_IOTYPE_MAP, configurableBlock.getIOMap()); // ✅ 寫入新的 IOType map
                        });


                        return InteractionResult.SUCCESS;
                    }
                    return InteractionResult.PASS;
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

        EnumMap<Direction, Boolean> config = stack.get(ModDataComponents.CONFIGURED_DIRECTIONS);
//        MagicalIndustryMod.LOGGER.info("[Tooltip] Stack: {}, HasConfig = {}", stack.getItem(), config != null);

        if (config != null && !config.isEmpty()) {
            // 用 Map<Boolean, List<Direction>> 來分類
            Map<Boolean, List<Direction>> groupedDirections = new HashMap<>();
            groupedDirections.put(true, new ArrayList<>());
            groupedDirections.put(false, new ArrayList<>());

            for (Direction dir : Direction.values()) {
                boolean isOutput = config.getOrDefault(dir, false); // 預設為輸入
                groupedDirections.get(isOutput).add(dir);
            }

            // 加入提示
            for (Map.Entry<Boolean, List<Direction>> entry : groupedDirections.entrySet()) {
                List<Direction> dirs = entry.getValue();
                if (!dirs.isEmpty()) {
                    String names = dirs.stream()
                            .map(Direction::getName)
                            .map(String::toLowerCase)
                            .collect(Collectors.joining(", "));
                    String configType = entry.getKey() ? "output" : "input";
                    tooltipComponents.add(Component.translatable("tooltip.magical_industry.saved_direction_config", names,
                            Component.translatable("screen.magical_industry." + configType)
                    ));
                }
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.magical_industry.no_saved_block"));
        }

    }

    public enum TechWandMode implements StringRepresentable {
        CONFIGURE_IO,
        DIRECTION_CONFIG,
        ROTATE;

        public TechWandMode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        public TechWandMode previous() {
            return values()[(this.ordinal() - 1 + values().length) % values().length];
        }

        public TechWandMode cycle(boolean forward) {
            TechWandMode[] values = values();
            int index = this.ordinal();
            int nextIndex = (index + (forward ? 1 : -1) + values.length) % values.length;
            return values[nextIndex];
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }

    }
}
