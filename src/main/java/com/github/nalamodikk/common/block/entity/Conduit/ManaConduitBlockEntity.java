package com.github.nalamodikk.common.block.entity.Conduit;

import com.github.nalamodikk.common.mana.ManaAction;
import com.github.nalamodikk.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import com.github.nalamodikk.common.Capability.*;

import javax.annotation.Nullable;

public class ManaConduitBlockEntity extends BlockEntity {
    private final ManaStorage manaStorage = new ManaStorage(1000); // 最大存 1000 魔力
    private final LazyOptional<IUnifiedManaHandler> manaOptional = LazyOptional.of(() -> manaStorage);

    public ManaConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_CONDUIT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ManaConduitBlockEntity entity) {
        if (level.isClientSide) return; // 只在伺服器端運行

        if (level.getGameTime() % 10 == 0) { // 每 10 tick 嘗試傳遞魔力
            for (Direction direction : Direction.values()) {
                BlockEntity be = level.getBlockEntity(pos.relative(direction));
                if (be instanceof ManaConduitBlockEntity neighbor) {
                    entity.transferMana(neighbor);
                }
            }
        }
    }



    private void transferMana(ManaConduitBlockEntity target) {
        int amount = Math.min(50, this.manaStorage.getMana()); // 每次最多傳 50 魔力

        // 修正 extractMana & receiveMana，添加 ManaAction.EXECUTE
        int extracted = this.manaStorage.extractMana(amount, ManaAction.EXECUTE);
        int received = target.manaStorage.receiveMana(extracted, ManaAction.EXECUTE);
    }




    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == ModCapabilities.MANA ? manaOptional.cast() : super.getCapability(cap, side);
    }
}
