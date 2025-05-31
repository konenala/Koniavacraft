package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;


@EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapability {
    public static final BlockCapability<IUnifiedManaHandler, Direction> MANA =
            BlockCapability.create(
                    ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "mana"),
                    IUnifiedManaHandler.class,
                    Direction.class
            );



    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ModCapability.MANA, ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), (blockEntity, side) -> blockEntity.getManaStorage());
        event.registerBlockEntity(ModCapability.MANA, ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), (blockEntity, side) -> blockEntity.getManaStorage());
        event.registerBlockEntity(ModCapability.MANA, ModBlockEntities.MANA_GENERATOR_BE.get(), (blockEntity, side) -> blockEntity.getManaStorage());

        // 物品欄能力：ManaCraftingTableBlockEntity;
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_CRAFTING_TABLE_BLOCK_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.SOLAR_MANA_COLLECTOR_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MANA_GENERATOR_BE.get(), (blockEntity, side) -> blockEntity.getItemHandler());
    }

}

