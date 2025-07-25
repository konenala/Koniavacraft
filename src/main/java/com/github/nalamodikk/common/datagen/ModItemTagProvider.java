package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, KoniavacraftMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana")))
                .add(
                        ModItems.CONDENSED_MANA_DUST.get(),
                        ModItems.CORRUPTED_MANA_DUST.get(),
                        ModItems.MANA_DUST.get(),
                        ModBlocks.MANA_BLOCK.get().asItem(),
                        ModItems.MANA_INGOT.get(),
                        ModItems.MANA_CRYSTAL_FRAGMENT.get(),
                        ModItems.REFINED_MANA_DUST.get(),
                        ModItems.RAW_MANA_DUST.get()
                );

        tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "mana")))
                .add(

                        ModItems.RAW_MANA_DUST.get()
                );

    }
}
