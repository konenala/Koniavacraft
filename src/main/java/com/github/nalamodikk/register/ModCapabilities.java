package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;

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


@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
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
        event.registerBlockEntity(ModCapabilities.MANA, ModBlockEntities.MANA_GENERATOR_BE.get(), (blockEntity, side) -> blockEntity.getManaStorage());

        // 導管能力
        event.registerBlockEntity(ModCapabilities.MANA, ModBlockEntities.ARCANE_CONDUIT_BE.get(), (blockEntity, side) -> blockEntity);
        // 物品欄能力：ManaCraftingTableBlockEntity;
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_GENERATOR_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());


        // 實體能力
//        event.registerEntity(ModCapability.NARA,EntityType.PLAYER, (player, ctx) -> new NaraData());

    }

}

