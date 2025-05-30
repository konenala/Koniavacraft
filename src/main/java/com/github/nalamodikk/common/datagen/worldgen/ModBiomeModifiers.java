package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModBiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_MAGIC_ORE = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "add_magic_ore"));

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);

        BiomeModifier modifier = new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD), // 主世界所有 Biome
                HolderSet.direct(placed.getOrThrow(ModPlacedFeatures.MAGIC_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES // 掛在什麼階段生成
        );

        context.register(ADD_MAGIC_ORE, modifier);
    }
}
