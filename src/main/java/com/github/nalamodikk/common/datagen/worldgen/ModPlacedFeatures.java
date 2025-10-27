package com.github.nalamodikk.common.datagen.worldgen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.datagen.worldgen.ore.ModOrePlacement;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> MAGIC_ORE_PLACED_KEY = registerKey("magic_ore");
    public static final ResourceKey<PlacedFeature> MANA_BLOOM_PLACED_KEY = registerKey("mana_bloom_patch");

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, MAGIC_ORE_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.MAGIC_ORE_KEY),
                ModOrePlacement.commonOrePlacement(10, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(64)))
        );

        register(context, MANA_BLOOM_PLACED_KEY,
                configuredFeatures.getOrThrow(ModConfiguredFeatures.MANA_BLOOM_PATCH_KEY),
                List.of(CountPlacement.of(3), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome()));
    }


    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }


    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, name));
    }

}
