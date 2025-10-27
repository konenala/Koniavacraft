package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> MAGIC_ORE_KEY = registerKey("magic_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MANA_BLOOM_PATCH_KEY = registerKey("mana_bloom_patch");

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        List<OreConfiguration.TargetBlockState> magicOreTargets = List.of(
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), ModBlocks.MAGIC_ORE.get().defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), ModBlocks.DEEPSLATE_MAGIC_ORE.get().defaultBlockState())
        );

        register(context, MAGIC_ORE_KEY, Feature.ORE, new OreConfiguration(magicOreTargets, 15));

        Holder<ConfiguredFeature<?, ?>> manaBloomBlock = Holder.direct(new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(ModBlocks.MANA_BLOOM.get().defaultBlockState()))));
        Holder<PlacedFeature> manaBloomPlaced = Holder.direct(new PlacedFeature(
                manaBloomBlock,
                List.of(PlacementUtils.filteredByBlockSurvival(ModBlocks.MANA_BLOOM.get()))
        ));
        RandomPatchConfiguration manaBloomPatch = new RandomPatchConfiguration(32, 6, 2, manaBloomPlaced);
        register(context, MANA_BLOOM_PATCH_KEY, Feature.RANDOM_PATCH, manaBloomPatch);
    }


    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register
            (BootstrapContext<ConfiguredFeature<?, ?>> context,ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
