package com.github.nalamodikk.common.utils;

import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.BiFunction;
import java.util.function.Function;


public class ModCapabilityUtils {

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