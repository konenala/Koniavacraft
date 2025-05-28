package com.github.nalamodikk.common.datagen;


import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.register.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, MagicalIndustryMod.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.MANA_DEBUG_TOOL.get());
        basicItem(ModItems.BASIC_TECH_WAND);
        basicItem(ModItems.MANA_DUST.get());
        basicItem(ModItems.MANA_INGOT.get());
        basicItem(ModItems.CORRUPTED_MANA_DUST.get());
    }

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID,"block/" + item.getId().getPath()));
    }

    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID,"item/" + item.getId().getPath()));
    }
}
