package com.github.nalamodikk.common.block.entity.basic;

import com.github.nalamodikk.common.block.entity.AbstractManaCollectorMachine;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

/**
 * ☀️ 太陽能原能收集器
 * - 僅在白天且可見天空時產生魔力
 * - 高海拔（Y > 100）加成效率（額外產能）
 */
public class SolarManaCollectorBlockEntity extends AbstractManaCollectorMachine {

    private static final int BASE_INTERVAL = 40;       // 每 40 tick 嘗試一次（2 秒）
    private static final int BASE_OUTPUT = 5;          // 每次產出 5 mana（晴天正常條件）
    private static final int MAX_MANA = 80000;          // 儲存上限

    public SolarManaCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), pos, state, MAX_MANA, BASE_INTERVAL, BASE_OUTPUT);
    }

    /**
     * 判斷是否符合太陽能條件：
     * - 白天
     * - 非下雨/雷雨
     * - 天空可見
     */
    @Override
    protected boolean canGenerate() {
        if (!level.isDay()) return false;
        if (level.isRaining()) return false;
        if (!level.canSeeSky(worldPosition.above())) return false;
        return true;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }

    @Override
    public void setDirectionConfig(Direction direction, boolean isOutput) {

    }

    @Override
    public boolean isOutput(Direction direction) {
        return false;
    }

    @Override
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(0); // 目前沒槽，未來支援升級可擴充
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }



    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magical_industry.solar_mana_collector");
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }


    public ManaStorage getManaStorage() {
        return this.manaStorage;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return null;
    }
}
