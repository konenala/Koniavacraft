package com.github.nalamodikk.item.debug;

import com.github.nalamodikk.Capability.IMana;
import com.github.nalamodikk.Capability.ManaCapability;
import com.github.nalamodikk.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@Mod.EventBusSubscriber
public class ManaDebugToolItem extends Item {

    private static final int[] MANA_AMOUNTS = {10, 100, 1000};
    private static final String[] MODES = {" 10 ", " 100 ", " 1000 "};
    private int modeIndex = 0;

    public ManaDebugToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity != null) {
                // 檢查方塊實體是否具有魔力系統
                blockEntity.getCapability(ManaCapability.MANA).ifPresent(manaStorage -> {
                    // 增加魔力
                    int manaToAdd = MANA_AMOUNTS[modeIndex];
                    manaStorage.addMana(manaToAdd);
                    System.out.println("Debug: Added " + manaToAdd + " Mana. Current Mana: " + manaStorage.getMana());

                    // 向玩家顯示訊息
                    Player player = context.getPlayer();
                    if (player != null) {
                        player.displayClientMessage(Component.translatable("message.magical_industry.mana_added", manaToAdd, manaStorage.getMana()), true);
                    }

                    // 確保狀態已更新
                    blockEntity.setChanged();
                    BlockState state = level.getBlockState(pos);
                    level.sendBlockUpdated(pos, state, state, 3);
                });

                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (heldItem.getItem() instanceof ManaDebugToolItem && player.isCrouching()) {
                ManaDebugToolItem manaDebugToolItem = (ManaDebugToolItem) heldItem.getItem();
                manaDebugToolItem.cycleMode();
                player.displayClientMessage(Component.translatable("message.magical_industry.mana_mode_changed", manaDebugToolItem.getCurrentModeDescription()), true);
                event.setCanceled(true); // 阻止玩家切換物品欄位
            }
        }
    }

    private void cycleMode() {
        modeIndex = (modeIndex + 1) % MANA_AMOUNTS.length;
    }

    private String getCurrentModeDescription() {
        return MODES[modeIndex];
    }
}
