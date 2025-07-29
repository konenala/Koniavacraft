package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.coreapi.block.IConfigurableBlock;
import com.github.nalamodikk.common.utils.capability.IOHandlerUtils;
import com.github.nalamodikk.common.utils.capability.RestrictedManaHandler;
import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModCapabilities {
    public static final BlockCapability<IUnifiedManaHandler, Direction> MANA =
            BlockCapability.create(
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana"),
                    IUnifiedManaHandler.class,
                    Direction.class
            );

    public static final DataComponentType<Boolean> NARA_BOUND =
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build();



//    public static final EntityCapability<INaraData, Void> NARA =
//            EntityCapability.createVoid(
//                    ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "nara_data"),
//                    INaraData.class
//            );




    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 方塊機器能力
        event.registerBlockEntity(ModCapabilities.MANA, ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), (blockEntity, side) -> blockEntity.getManaStorage());
        event.registerBlockEntity(ModCapabilities.MANA, ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), (blockEntity, side) -> blockEntity.getManaStorage());

        // 魔力能力 - 根據 IO 配置決定功能
        event.registerBlockEntity(ModCapabilities.MANA, ModBlockEntities.MANA_GENERATOR_BE.get(),
                (blockEntity, side) -> {
                    if (side != null && blockEntity instanceof IConfigurableBlock configurable) {
                        IOHandlerUtils.IOType ioType = configurable.getIOConfig(side);

                        return switch (ioType) {
                            case DISABLED -> null; // 禁用面不提供能力
                            case INPUT -> new RestrictedManaHandler(blockEntity.getManaStorage(), true, false); // 只能接收
                            case OUTPUT -> new RestrictedManaHandler(blockEntity.getManaStorage(), false, true); // 只能被抽取
                            case BOTH -> blockEntity.getManaStorage(); // 完整功能
                        };
                    }
                    return blockEntity.getManaStorage(); // 默認完整功能
                });

        // 物品能力 - 根據 IO 配置決定功能
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_GENERATOR_BE.get(),
                (blockEntity, side) -> {
                    if (side != null && blockEntity instanceof IConfigurableBlock configurable) {
                        IOHandlerUtils.IOType ioType = configurable.getIOConfig(side);
                        if (ioType == IOHandlerUtils.IOType.DISABLED) {
                            return null; // 該面禁用，不提供能力
                        }
                    }
                    return blockEntity.getInventory(); // 🔧 修正：使用 getInventory()
                });
        // 導管能力
        event.registerBlockEntity(ModCapabilities.MANA, ModBlockEntities.ARCANE_CONDUIT_BE.get(), (blockEntity, side) -> blockEntity);
        // 物品欄能力：ManaCraftingTableBlockEntity;
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());

        //rf能量註冊
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.MANA_GENERATOR_BE.get(),
                (blockEntity, side) -> {
                    if (side != null && blockEntity instanceof IConfigurableBlock configurable) {
                        IOHandlerUtils.IOType ioType = configurable.getIOConfig(side);
                        if (ioType == IOHandlerUtils.IOType.DISABLED) {
                            return null; // 該面禁用，不提供能力
                        }
                        if (ioType == IOHandlerUtils.IOType.INPUT) {
                            return null; // 發電機不應該從輸入面接收能量
                        }
                    }
                    return blockEntity.getEnergyStorage(); // 🔧 返回你的能量儲存
                });
        // 實體能力
//        event.registerEntity(ModCapability.NARA,EntityType.PLAYER, (player, ctx) -> new NaraData());
        // 🆕 魔力注入機能力註冊
        // 魔力能力 - 根據 IO 配置決定功能
        event.registerBlockEntity(ModCapabilities.MANA, ModBlockEntities.MANA_INFUSER.get(),
                (blockEntity, side) -> {
                    if (side != null && blockEntity instanceof IConfigurableBlock configurable) {
                        IOHandlerUtils.IOType ioType = configurable.getIOConfig(side);

                        return switch (ioType) {
                            case DISABLED -> null; // 禁用面不提供能力
                            case INPUT -> new RestrictedManaHandler(blockEntity.getManaStorage(), true, false); // 只能接收
                            case OUTPUT -> new RestrictedManaHandler(blockEntity.getManaStorage(), false, true); // 只能被抽取
                            case BOTH -> blockEntity.getManaStorage(); // 完整功能
                        };
                    }
                    return blockEntity.getManaStorage(); // 默認完整功能
                });

        // 🆕 物品處理能力 - 根據 IO 配置決定功能
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_INFUSER.get(),
                (blockEntity, side) -> {
                    if (side != null && blockEntity instanceof IConfigurableBlock configurable) {
                        IOHandlerUtils.IOType ioType = configurable.getIOConfig(side);
                        if (ioType == IOHandlerUtils.IOType.DISABLED) {
                            return null; // 該面禁用，不提供能力
                        }
                    }
                    return blockEntity.getItemHandler();
                });

    }

}

