package com.github.nalamodikk.common.block.conduit;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ArcaneConduitBlock extends BaseEntityBlock {

    // 6個方向的連接屬性
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    // 導管形狀 - 中心 + 6個方向的連接
    private static final VoxelShape CENTER = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape NORTH_SHAPE = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_SHAPE = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape EAST_SHAPE = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape UP_SHAPE = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape DOWN_SHAPE = Block.box(6, 0, 6, 10, 6, 10);

    public ArcaneConduitBlock(Properties properties) {
        super(properties);
        // 預設所有方向都不連接
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(EAST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;

        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);

        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneConduitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == ModBlockEntities.ARCANE_CONDUIT_BE.get() ?
                (level1, pos, state1, blockEntity) -> ((ArcaneConduitBlockEntity) blockEntity).tick() : null;
    }



    // 檢查是否應該連接到某個方向
    public boolean shouldConnectTo(Level level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockEntity neighborBE = level.getBlockEntity(neighborPos);

        // 連接到其他導管
        if (neighborBE instanceof ArcaneConduitBlockEntity) {
            return true;
        }

        // 連接到有魔力能力的機器
        if (neighborBE != null) {
            // 這裡檢查是否有 MANA capability
            return level.getCapability(com.github.nalamodikk.register.ModCapabilities.MANA, neighborPos, direction.getOpposite()) != null;
        }

        return false;
    }

    // 更新連接狀態

    // 你還需要確保有這個方法：

    public BlockState updateConnections(Level level, BlockPos pos, BlockState state) {
        BlockState newState = state;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            boolean shouldConnect = canConnectTo(level, neighborPos, direction);

            // 根據你的 ArcaneConduitBlock 屬性名稱調整
            BooleanProperty property = switch (direction) {
                case NORTH -> ArcaneConduitBlock.NORTH;
                case SOUTH -> ArcaneConduitBlock.SOUTH;
                case WEST -> ArcaneConduitBlock.WEST;
                case EAST -> ArcaneConduitBlock.EAST;
                case UP -> ArcaneConduitBlock.UP;
                case DOWN -> ArcaneConduitBlock.DOWN;
            };

            newState = newState.setValue(property, shouldConnect);
        }

        return newState;
    }


    private boolean canConnectTo(Level level, BlockPos pos, Direction direction) {
        // 🔧 【重要】：檢查自己的IO配置，如果該方向是DISABLED則不連接
        BlockEntity thisBE = level.getBlockEntity(pos.relative(direction.getOpposite()));
        if (thisBE instanceof ArcaneConduitBlockEntity thisConduit) {
            IOHandlerUtils.IOType thisConfig = thisConduit.getIOConfig(direction);
            if (thisConfig == IOHandlerUtils.IOType.DISABLED) {
                return false; // 該方向已禁用，不顯示連接
            }
        }

        // 檢查是否應該連接到該位置
        // 1. 是否為其他導管
        if (level.getBlockEntity(pos) instanceof ArcaneConduitBlockEntity) {
            return true;
        }

        // 2. 是否有魔力能力
        IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(level, pos, direction);
        return handler != null;
    }
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            // 更新視覺連接
            BlockState newState = updateConnections(level, pos, state);
            if (newState != state) {
                level.setBlock(pos, newState, 3);
            }

            // 🔧 關鍵：通知 BlockEntity 鄰居變化
            if (level.getBlockEntity(pos) instanceof ArcaneConduitBlockEntity conduit) {
                conduit.onNeighborChanged();
            }
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }






    /**
     * 🆕 判斷是否為方塊物品
     */
    private boolean isBlockItem(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem;
    }


    private InteractionResult tryPlaceBlock(Player player, ItemStack heldItem) {
        // 🎯 策略1：完全讓出控制權給原版放置邏輯
        // 返回 PASS 會讓 Minecraft 的原版放置邏輯接管
        return InteractionResult.PASS;

        // 🎯 策略2：如果你想要控制放置行為，可以用以下代碼：
    /*
    if (heldItem.getItem() instanceof BlockItem blockItem) {
        Block block = blockItem.getBlock();

        // 檢查是否允許在這裡放置這種方塊
        if (isAllowedToPlace(block)) {
            // 讓原版邏輯處理放置
            return InteractionResult.PASS;
        } else {
            // 阻止放置，顯示訊息
            player.displayClientMessage(
                Component.translatable("message.koniava.cannot_place_here",
                    block.getName()),
                true
            );
            return InteractionResult.FAIL;
        }
    }
    return InteractionResult.PASS;
    */
    }


    /**
     * 🔧 完整的 useWithoutItem 方法實現
     * 加入到你現有的 ArcaneConduitBlock 類中
     */

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                            Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof ArcaneConduitBlockEntity conduit) {
            ItemStack heldItem = player.getMainHandItem();
            boolean isCrouching = player.isCrouching();

            // 🎯 情況1：手持科技魔杖 - 使用魔杖的邏輯（最高優先級）
            if (heldItem.getItem() instanceof com.github.nalamodikk.common.item.tool.BasicTechWandItem) {
                return conduit.onUse(state, level, pos, player, hit);
            }

            // 🆕 情況2：手持方塊物品 = 放置方塊（無論是否蹲下）
            if (!heldItem.isEmpty() && isBlockItem(heldItem)) {
                return tryPlaceBlock(player, heldItem);
            }

            // 🎯 情況3：空手或手持其他物品
            return handleEmptyHandInteraction(conduit, player, heldItem);
        }

        return InteractionResult.PASS;
    }


    /**
     * 🆕 缺少的 isAllowedToPlace 方法（如果你想要控制放置行為）
     */
    private boolean isAllowedToPlace(Block block) {
        // 你可以在這裡定義哪些方塊不允許放置在導管上

        // 示例：禁止放置流體
        if (block.defaultBlockState().liquid()) {
            return false;
        }

        // 示例：禁止放置其他導管類型
        if (block instanceof ArcaneConduitBlock) {
            return false;
        }

        // 默認允許放置
        return true;
    }

    /**
     * 處理空手或手持其他物品時的交互
     */
    private InteractionResult handleEmptyHandInteraction(ArcaneConduitBlockEntity conduit, Player player, ItemStack heldItem) {
        boolean isEmptyHand = heldItem.isEmpty();
        boolean isCrouching = player.isCrouching();

        // 🎯 優先級順序：
        // 1. Shift + 空手 → 打開配置 GUI（最高優先級）
        // 2. 空手 → 顯示快速信息
        // 3. Shift + 手持非方塊物品 → 嘗試打開配置 GUI
        // 4. 手持非方塊物品 → 顯示基本信息或執行其他邏輯

        if (isCrouching && isEmptyHand) {
            // 🔧 最佳體驗：Shift + 空手 = 配置 GUI
            return openConfigurationGUI(conduit, player);
        }

        if (isEmptyHand) {
            // 🔧 空手右鍵 = 快速信息
            showQuickInfo(conduit, player);
            return InteractionResult.SUCCESS;
        }

        if (isCrouching && !isBlockItem(heldItem)) {
            // 🔧 Shift + 手持非方塊物品 = 也能打開配置（便利性）
            return openConfigurationGUI(conduit, player);
        }

        // 🔧 手持非方塊物品 + 普通右鍵 = 嘗試使用物品或顯示信息
        return handleItemInteraction(conduit, player, heldItem);
    }

    /**
     * 打開配置 GUI
     */
    private InteractionResult openConfigurationGUI(ArcaneConduitBlockEntity conduit, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider menuProvider = new SimpleMenuProvider(
                    (id, inventory, p) -> new ArcaneConduitConfigMenu(id, inventory, conduit),
                    Component.translatable("gui.koniava.conduit_config")
            );

            serverPlayer.openMenu(menuProvider, extraData -> {
                extraData.writeBlockPos(conduit.getBlockPos());
            });

            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 顯示快速信息
     */
    private void showQuickInfo(ArcaneConduitBlockEntity conduit, Player player) {
        int manaStored = conduit.getManaStored();
        int maxMana = conduit.getMaxManaStored();
        int connections = conduit.getActiveConnectionCount();

        // 🔧 添加更多有用信息
        player.displayClientMessage(
                Component.translatable("message.koniava.conduit.quick_info",
                        manaStored, maxMana, connections),
                true // 顯示在 ActionBar
        );

        // 🆕 可選：在聊天中顯示詳細信息
        if (player.isCrouching()) {
            showDetailedInfo(conduit, player);
        }
    }



  /*
   * 處理手持物品時的交互
   */
    private InteractionResult handleItemInteraction(ArcaneConduitBlockEntity conduit, Player player, ItemStack heldItem) {
        // 🆕 1. 判斷是否為方塊物品，如果是則嘗試放置方塊
        if (isBlockItem(heldItem)) {
            return tryPlaceBlock(player, heldItem);
        }

        // TODO: 2. 手持魔力相關物品時可以直接充能
//    if (isManualManaItem(heldItem)) {
//        return tryManualManaTransfer(conduit, player, heldItem);
//    }

        // 3. 手持工具時顯示技術信息
        if (isTechnicalTool(heldItem)) {
            showTechnicalInfo(conduit, player);
            return InteractionResult.SUCCESS;
        }

        // 🔧 4. 默認：顯示基本信息
        showQuickInfo(conduit, player);
        return InteractionResult.SUCCESS;
    }
    /**
     * 顯示詳細信息（可選功能）
     */
    private void showDetailedInfo(ArcaneConduitBlockEntity conduit, Player player) {
        // 🆕 顯示每個方向的配置
        player.displayClientMessage(Component.translatable("message.koniava.conduit.info_header"), false);

        for (Direction dir : Direction.values()) {
            IOHandlerUtils.IOType type = conduit.getIOConfig(dir);
            int priority = conduit.getPriority(dir);

            if (type != IOHandlerUtils.IOType.DISABLED) {
                player.displayClientMessage(Component.translatable(
                        "message.koniava.conduit.detailed_config",
                        Component.translatable("direction.koniava." + dir.name().toLowerCase()),
                        Component.translatable("mode.koniava." + type.name().toLowerCase()),
                        priority
                ), false);
            }
        }
    }

    // 🔧 輔助方法



    private boolean isTechnicalTool(ItemStack stack) {
        // 檢查是否為技術工具（除了科技魔杖之外）
        return stack.getItem().toString().contains("debug") ||
                stack.getItem().toString().contains("analyzer");
    }

    private InteractionResult tryManualManaTransfer(ArcaneConduitBlockEntity conduit, Player player, ItemStack heldItem) {
        // TODO: 實現手動魔力傳輸功能
        // - 檢查物品魔力容量
        // - 計算傳輸量
        // - 消耗物品或減少物品魔力
        // - 給導管充能
        // - 顯示傳輸成功信息

        player.displayClientMessage(Component.translatable("message.koniava.manual_mana_transfer_todo"), true);
        return InteractionResult.SUCCESS;
    }


    private void showTechnicalInfo(ArcaneConduitBlockEntity conduit, Player player) {
        // 🆕 顯示技術信息：網路ID、傳輸速率、性能統計等
        player.displayClientMessage(Component.translatable(
                "message.koniava.conduit.technical_info",
                conduit.getBlockPos().toString(),
                conduit.getManaStored(),
                "TODO: 網路統計"
        ), false);
    }
}