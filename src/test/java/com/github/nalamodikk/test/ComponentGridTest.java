package com.github.nalamodikk.test;

import com.github.nalamodikk.common.API.machine.behavior.ManaProducerBehavior;
import com.github.nalamodikk.common.API.machine.grid.ComponentContext;
import com.github.nalamodikk.common.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.API.IComponentBehavior;
import com.github.nalamodikk.common.API.IGridComponent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentGridTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("TestLogger");

    @Test
    public void testManaTransferBetweenProducerAndStorage() {
        // 建立 Grid
        ComponentGrid grid = new ComponentGrid();

        // 建立行為與設定 NBT
        ManaProducerBehavior behavior = new ManaProducerBehavior();
        CompoundTag tag = new CompoundTag();
        tag.putInt("mana_per_tick", 10);
        behavior.init(tag);

        // 建立元件並放進 grid
        BlockPos producerPos = new BlockPos(0, 0, 0);
        BlockPos receiverPos = new BlockPos(1, 0, 0);

        grid.setComponent(producerPos.getX(), producerPos.getZ(), new TestComponent("mana_producer", behavior));
        DummyManaReceiverComponent receiver = new DummyManaReceiverComponent();
        grid.setComponent(receiverPos.getX(), receiverPos.getZ(), receiver);

        // 模擬 tick
        for (int i = 0; i < 5; i++) {
            IGridComponent comp = grid.getComponent(producerPos.getX(), producerPos.getZ());
            if (comp instanceof IComponentBehavior behaviorComp) {
                ComponentContext ctx = new ComponentContext(grid, producerPos, comp);
                behaviorComp.onTick(ctx);
            }
        }

        int actualMana = receiver.getManaStorage().getMana();
        LOGGER.info("接收到 mana: {}", actualMana);
        Assertions.assertEquals(50, actualMana, "Mana should be 50 after 5 ticks of transfer");
    }

    // 測試用包裝器
    // 測試用包裝器：同時是 IGridComponent + IComponentBehavior
    public record TestComponent(String id, IComponentBehavior behavior) implements IGridComponent, IComponentBehavior {
        @Override
        public void onTick(ComponentContext context) {
            behavior.onTick(context); // 轉呼叫原本的行為邏輯
        }

        @Override
        public net.minecraft.resources.ResourceLocation getId() {
            return new net.minecraft.resources.ResourceLocation("magical_industry", id);
        }

        @Override public void onAdded(ComponentGrid grid, BlockPos pos) {}
        @Override public void onRemoved(ComponentGrid grid, BlockPos pos) {}
        @Override public void saveToNBT(CompoundTag tag) {}
        @Override public void loadFromNBT(CompoundTag tag) {}
        @Override public CompoundTag getData() { return new CompoundTag(); }
    }

}
