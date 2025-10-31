package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.biome.ModBiomes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBiomeTagProvider extends BiomeTagsProvider {
    public ModBiomeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, KoniavacraftMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 創建自訂標籤，包含平原、森林   等生物群落
        tag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,
                        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "has_mana_bloom")))
                // 平原生物群落
                .add(Biomes.PLAINS)
                .add(Biomes.SUNFLOWER_PLAINS)
                .add(Biomes.MEADOW)
                // 森林生物群落
                .add(Biomes.FOREST)
                .add(Biomes.FLOWER_FOREST)
                .add(Biomes.BIRCH_FOREST)
                .add(Biomes.OLD_GROWTH_BIRCH_FOREST)
                .add(Biomes.DARK_FOREST)
                .add(Biomes.GROVE)
                // 也可以添加其他生物群落
                .add(Biomes.SAVANNA)
                .add(Biomes.SAVANNA_PLATEAU)
                .add(Biomes.WINDSWEPT_SAVANNA)
                // 🌟 添加自訂生物群落 - 魔力草原
                .addOptional(ModBiomes.MANA_PLAINS.location());
    }
}
