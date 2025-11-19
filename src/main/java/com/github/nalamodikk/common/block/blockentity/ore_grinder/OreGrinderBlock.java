package com.github.nalamodikk.common.block.blockentity.ore_grinder;

import com.github.nalamodikk.common.block.blockentity.manabase.BaseMachineBlock;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.register.ModBlockEntities;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import javax.annotation.Nullable;

/**
 * ⚙️ 礦石粉碎機方塊
 *
 * 繼承自 BaseMachineBlock，獲得：
 * - 標準的方塊實體管理
 * - 朝向屬性支援
 * - 標準的掉落物處理
 *
 * 功能：
 * - 消耗魔力來粉碎礦物
 * - 支援多輸入槽位
 * - 產生多個輸出物品（包含概率副產物）
 * - IO 方向可配置
 */
public class OreGrinderBlock extends BaseMachineBlock {

    private static final Logger LOGGER = LogUtils.getLogger();

    // 🔧 額外的方塊屬性
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    // 📐 方塊形狀
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

    // 🎨 MapCodec (NeoForge 1.21.1 要求)
    public static final MapCodec<OreGrinderBlock> CODEC = simpleCodec(OreGrinderBlock::new);

    public OreGrinderBlock(Properties properties) {
        super(properties);
        // 註冊默認狀態（包含基類的 FACING 和我們的 WORKING）
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WORKING, false));
    }

    @Override
    protected MapCodec<OreGrinderBlock> codec() {
        return CODEC;
    }

    // === 🏗️ 方塊屬性（擴展基類）===
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 方塊的"正面"面向玩家
        Direction facing = context.getHorizontalDirection().getOpposite();

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WORKING, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // 添加 FACING
        builder.add(WORKING);                      // 添加 WORKING
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // === 🔧 BlockEntity 管理 ===

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreGrinderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.ORE_GRINDER.get(),
                (level1, pos, state1, blockEntity) -> {
                    if (!level1.isClientSide()) {
                        blockEntity.tickMachine();
                    }
                });
    }

    // === 🎮 玩家交互 ===

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof OreGrinderBlockEntity grinder) {

            // 🔧 如果拿著科技魔杖，打開配置界面
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.getItem() instanceof BasicTechWandItem) {
                return handleWandInteraction(player, grinder, heldItem, hitResult.getDirection());
            }

            // 🖥️ 否則打開主界面
            openOreGrinderMenu((ServerPlayer) player, grinder);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 🔧 處理科技魔杖交互
     */
    private InteractionResult handleWandInteraction(Player player, OreGrinderBlockEntity grinder, ItemStack wand, Direction hitSide) {
        if (player.isCrouching()) {
            // Shift + 右鍵：循環切換 IO 模式 (DISABLED -> INPUT -> OUTPUT -> BOTH -> DISABLED)
            IOHandlerUtils.IOType currentMode = grinder.getIOConfig(hitSide);
            IOHandlerUtils.IOType nextMode = switch (currentMode) {
                case DISABLED -> IOHandlerUtils.IOType.INPUT;
                case INPUT -> IOHandlerUtils.IOType.OUTPUT;
                case OUTPUT -> IOHandlerUtils.IOType.BOTH;
                case BOTH -> IOHandlerUtils.IOType.DISABLED;
            };

            grinder.setIOConfig(hitSide, nextMode);

            String modeName = nextMode.name();
            player.sendSystemMessage(Component.translatable("message.koniava.io_mode_changed")
                    .append(Component.literal(": " + modeName))
                    .withStyle(ChatFormatting.GREEN));
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 🖥️ 打開粉碎機界面
     */
    private void openOreGrinderMenu(ServerPlayer player, OreGrinderBlockEntity grinder) {
        MenuProvider menuProvider = new SimpleMenuProvider(
                (id, playerInventory, accessPlayer) -> grinder.createMenu(id, playerInventory, accessPlayer),
                grinder.getDisplayName()
        );

        player.openMenu(menuProvider, grinder.getBlockPos());
    }

    // === 🔄 方塊更新通知 ===

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        // 通知 BlockEntity 鄰居改變
        if (level.getBlockEntity(pos) instanceof OreGrinderBlockEntity grinder) {
            grinder.hasInputChanged = true;
        }
    }
}
