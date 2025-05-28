package com.github.nalamodikk.common.utils;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.register.ModCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;


public class CapabilityUtils {




    public static @Nullable IUnifiedManaHandler getMana(Level level, BlockPos pos, @Nullable Direction dir) {
        // 優先查詢給定方向
        IUnifiedManaHandler handler = level.getCapability(ModCapability.MANA, pos, dir);

        // 如果失敗，嘗試用 null fallback 查詢（有些註冊用 createVoid）
        if (handler == null) {
            handler = level.getCapability(ModCapability.MANA, pos, null);
        }

        return handler;
    }

    // 支援 Direction 作為 context 的能力（像是 ITEM_HANDLER）
    @SafeVarargs
    public static <T, BE extends BlockEntity> void registerBlockEntities(
            RegisterCapabilitiesEvent event,
            BlockCapability<T, net.minecraft.core.Direction> capability,
            BiFunction<BE, net.minecraft.core.Direction, T> providerFunc,
            BlockEntityType<? extends BE>... types
    ) {
        ICapabilityProvider<BE, net.minecraft.core.Direction, T> provider = providerFunc::apply;
        for (BlockEntityType<? extends BE> type : types) {
            event.registerBlockEntity(capability, type, provider);
        }
    }

    // 支援 Void 作為 context 的能力（像是自訂 mana 能力）
    @SafeVarargs
    public static <T, BE extends BlockEntity> void registerBlockEntitiesVoidContext(
            RegisterCapabilitiesEvent event,
            BlockCapability<T, Void> capability,
            Function<BE, T> providerFunc,
            BlockEntityType<? extends BE>... types
    ) {
        ICapabilityProvider<BE, Void, T> provider = (be, unused) -> providerFunc.apply(be);
        for (BlockEntityType<? extends BE> type : types) {
            event.registerBlockEntity(capability, type, provider);
        }
    }
}