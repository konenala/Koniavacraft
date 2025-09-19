package com.github.nalamodikk.common.block.blockentity.conduit;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.utils.capability.CapabilityUtils;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArcaneConduitBlock extends BaseEntityBlock {
    private static final Map<UUID, List<Long>> playerBuildingHistory = new ConcurrentHashMap<>();
    private static final long HISTORY_CLEANUP_INTERVAL = 300000; // 5分鐘
    private static long lastCleanup = 0;
    private static final double EDGE_CLICK_THRESHOLD = 0.35;
    private static final double CENTER_CLICK_THRESHOLD = 0.25;
    private static final int BUILDING_MODE_THRESHOLD = 3;
    private static final long BUILDING_MODE_TIME_WINDOW = 10000; // 1
    private static final Logger LOGGER = LogUtils.getLogger(); // ← 新增這行

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


    private static void cleanupOldHistory() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanup > HISTORY_CLEANUP_INTERVAL) {
            lastCleanup = currentTime;

            int originalSize = playerBuildingHistory.size();

            playerBuildingHistory.entrySet().removeIf(entry -> {
                List<Long> history = entry.getValue();
                history.removeIf(time -> currentTime - time > 30000); // 移除30秒前的記錄
                return history.isEmpty();
            });

            int cleanedSize = originalSize - playerBuildingHistory.size();
            if (cleanedSize > 0) {
                LOGGER.debug("Cleaned {} old player building history entries", cleanedSize);
            }
        }
    }


// === 修改2：清理方法更新 ===



    // ✅ 在 recordBlockPlacement 中調用清理
    public static void recordBlockPlacement(Player player) {
        cleanupOldHistory(); // 定期清理

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        playerBuildingHistory.computeIfAbsent(playerId, k -> new ArrayList<>()).add(currentTime);

        // 保持列表大小合理
        List<Long> history = playerBuildingHistory.get(playerId);
        if (history.size() > 10) {
            history.remove(0);
        }

        if (KoniavacraftMod.IS_DEV) {
            LOGGER.debug("Recorded block placement for player {}, history size: {}",
                    player.getGameProfile().getName(), history.size());
        }
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
            // ✅ 修復：應該從當前位置 pos 朝 direction 方向檢查，不是從 neighborPos
            boolean shouldConnect = canConnectTo(level, pos, direction);

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


    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide && !movedByPiston) {
            // 延遲1tick更新連接，讓放置音效先播放
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 執行延遲的連接更新
        updateConnectionsAfterPlacement(level, pos, state);
    }


    private void updateConnectionsAfterPlacement(Level level, BlockPos pos, BlockState state) {
        // 1. 更新自己的連接
        BlockState newState = updateConnections(level, pos, state);
        if (newState != state) {
            level.setBlock(pos, newState, 3);
        }

        // 2. 通知所有鄰居導管重新檢查連接
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBE = level.getBlockEntity(neighborPos);

            if (neighborBE instanceof ArcaneConduitBlockEntity) {
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof ArcaneConduitBlock) {
                    BlockState updatedNeighbor = updateConnections(level, neighborPos, neighborState);
                    if (updatedNeighbor != neighborState) {
                        level.setBlock(neighborPos, updatedNeighbor, 3);
                    }
                }
            }
        }
    }

    private boolean canConnectTo(Level level, BlockPos pos, Direction direction) {
        // 🔧 檢查自己的IO配置
        BlockEntity thisBE = level.getBlockEntity(pos);
        if (thisBE instanceof ArcaneConduitBlockEntity thisConduit) {
            IOHandlerUtils.IOType myConfig = thisConduit.getIOConfig(direction);
            if (myConfig == IOHandlerUtils.IOType.DISABLED) {
                return false; // 我自己禁用了這個方向
            }
        }

        // 🔧 檢查目標位置
        BlockPos targetPos = pos.relative(direction);

        // ✅ 修復：如果目標是導管，檢查對方配置後還要檢查是否有魔力能力
        if (level.getBlockEntity(targetPos) instanceof ArcaneConduitBlockEntity targetConduit) {
            Direction targetSide = direction.getOpposite();
            IOHandlerUtils.IOType targetConfig = targetConduit.getIOConfig(targetSide);

            // 如果對方禁用了對應面，不連接
            if (targetConfig == IOHandlerUtils.IOType.DISABLED) {
                return false;
            }

            // ✅ 雙方都允許，且目標是導管，所以可以連接
            return true;
        }

        // 🔧 如果目標不是導管，檢查是否有魔力能力
        IUnifiedManaHandler handler = CapabilityUtils.getNeighborMana(level, targetPos, direction);
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

// === 修改1：更新靜態方法調用 ===

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ArcaneConduitBlockEntity conduit) {
                // 🎯 只清理 Block 層級的緩存
                // BlockEntity 會在 setRemoved() 中清理自己的緩存
                clearPlayerBuildingHistory(); // 單獨的方法，只清理玩家建築歷史

                level.invalidateCapabilities(pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }



    // === 2. 清理玩家建築歷史（單個方塊移除時） ===
    private static void clearPlayerBuildingHistory() {
        // 可選：清理過期的玩家建築歷史，但不是全部清除
        cleanupOldHistory();
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

        if (!(level.getBlockEntity(pos) instanceof ArcaneConduitBlockEntity conduit)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getMainHandItem();
        boolean isCrouching = player.isCrouching();

        // 🎯 科技魔杖 - 永遠優先
        if (heldItem.getItem() instanceof com.github.nalamodikk.common.item.tool.BasicTechWandItem) {
            return conduit.onUse(state, level, pos, player, hit);
        }

        // 🎯 智能方塊處理
        if (!heldItem.isEmpty() && isBlockItem(heldItem)) {
            return handleSmartBlockPlacement(level, pos, player, hit, heldItem, isCrouching);
        }

        // 🎯 空手/其他物品
        if (isCrouching) {
            return openConfigurationGUI(conduit, player);
        } else {
            showQuickInfo(conduit, player);
            return InteractionResult.SUCCESS;
        }
    }



    private InteractionResult handleSmartBlockPlacement(Level level, BlockPos pos, Player player,
                                                        BlockHitResult hit, ItemStack blockItem,
                                                        boolean isCrouching) {
        // 🎯 智能檢測 1：點擊位置
        Vec3 hitLocation = hit.getLocation();
        Vec3 blockCenter = Vec3.atCenterOf(pos);
        double distanceFromCenter = hitLocation.subtract(blockCenter).length();

        boolean isEdgeClick = distanceFromCenter > EDGE_CLICK_THRESHOLD;
        boolean isCenterClick = distanceFromCenter < CENTER_CLICK_THRESHOLD;

        // 🎯 智能檢測 2：玩家建築模式
        boolean isInBuildingMode = isPlayerInBuildingMode(player);

        // 🎯 智能檢測 3：周圍環境
        boolean hasValidPlacement = canPlaceBlockNearby(level, pos, hit.getDirection(), blockItem);

        // 🎯 決策邏輯
        if (isCrouching || isEdgeClick || isInBuildingMode) {
            recordBlockPlacement(player); // ✅ 記錄放置行為
            return InteractionResult.PASS;
        } else if (isCenterClick && !hasValidPlacement) {
            showConduitInfoWithHint(level.getBlockEntity(pos), player, blockItem);
            return InteractionResult.SUCCESS;
        } else {
            showFriendlyChoice(level.getBlockEntity(pos), player, blockItem);
            return InteractionResult.SUCCESS;
        }
    }

    // 🎯 建築模式檢測
    private boolean isPlayerInBuildingMode(Player player) {
        cleanupOldHistory(); // 確保數據新鮮

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        List<Long> recentPlacements = playerBuildingHistory.get(playerId);
        if (recentPlacements == null || recentPlacements.isEmpty()) {
            return false;
        }

        // 移除過期記錄
        recentPlacements.removeIf(time -> currentTime - time > BUILDING_MODE_TIME_WINDOW);

        return recentPlacements.size() >= BUILDING_MODE_THRESHOLD;
    }


    // 🎯 檢查是否可以在附近放置
    private boolean canPlaceBlockNearby(Level level, BlockPos conduitPos, Direction hitSide, ItemStack blockItem) {
        if (blockItem.isEmpty() || !(blockItem.getItem() instanceof net.minecraft.world.item.BlockItem blockItemObj)) {
            return false;
        }

        BlockPos targetPos = conduitPos.relative(hitSide);

        // 檢查世界邊界
        if (!level.isInWorldBounds(targetPos)) {
            return false;
        }

        BlockState targetState = level.getBlockState(targetPos);

        // 檢查是否可以替換
        if (!targetState.canBeReplaced()) {
            return false;
        }

        // 檢查方塊是否可以放置在這個位置
        Block blockToPlace = blockItemObj.getBlock();
        BlockState stateToPlace = blockToPlace.defaultBlockState();

        return stateToPlace.canSurvive(level, targetPos);
    }

    // 🎯 顯示導管信息和溫和提示
    private void showConduitInfoWithHint(BlockEntity be, Player player, ItemStack blockItem) {
        if (!(be instanceof ArcaneConduitBlockEntity conduit)) return;

        try {
            // 顯示導管信息
            showQuickInfo(conduit, player);

            // ✅ 安全的名稱獲取
            Component blockNameComponent;
            if (blockItem.isEmpty()) {
                blockNameComponent = Component.translatable("misc.koniavacraft.unknown_block");
            } else {
                blockNameComponent = blockItem.getHoverName();
            }

            player.displayClientMessage(
                    Component.translatable("message.koniava.conduit.gentle_hint", blockNameComponent)
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
        } catch (Exception e) {
            // 容錯處理
            player.displayClientMessage(
                    Component.translatable("message.koniava.conduit.error_occurred")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    // 🎯 友好的選擇提示
    private void showFriendlyChoice(BlockEntity be, Player player, ItemStack blockItem) {
        if (!(be instanceof ArcaneConduitBlockEntity conduit)) return;

        String blockName = blockItem.getHoverName().getString();

        // 簡潔的雙選項
        player.displayClientMessage(
                Component.translatable("message.koniava.conduit.what_would_you_like", blockName)
                        .withStyle(ChatFormatting.YELLOW),
                false
        );

        player.displayClientMessage(
                Component.translatable("message.koniava.conduit.choice_info")
                        .withStyle(ChatFormatting.GRAY),
                false
        );

        player.displayClientMessage(
                Component.translatable("message.koniava.conduit.choice_place")
                        .withStyle(ChatFormatting.GREEN),
                false
        );
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