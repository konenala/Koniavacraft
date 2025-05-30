package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> MAGIC_ORE_PLACED_KEY = ResourceKey.create(
            Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "magic_ore"));

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

        PlacedFeature placed = new PlacedFeature(
                configured.getOrThrow(ModConfiguredFeatures.MAGIC_ORE_KEY),
                List.of(
                        CountPlacement.of(5), // 每區塊 5 次
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(12), VerticalAnchor.absolute(64)),
                        BiomeFilter.biome()
                )
        );

        context.register(MAGIC_ORE_PLACED_KEY, placed);
    }
}
