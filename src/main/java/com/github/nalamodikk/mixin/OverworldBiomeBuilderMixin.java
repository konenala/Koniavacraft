package com.github.nalamodikk.mixin;

import com.github.nalamodikk.biome.UniversalBiomeInjector;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Mixin for injecting custom biomes using UniversalBiomeInjector
 */
@Mixin(OverworldBiomeBuilder.class)
public class OverworldBiomeBuilderMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "addBiomes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/OverworldBiomeBuilder;addOffCoastBiomes(Ljava/util/function/Consumer;)V"))
    private void injectCustomBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, CallbackInfo ci) {
        LOGGER.info("🔧 OverworldBiomeBuilderMixin: Starting custom biome injection...");

        try {
            // 🌟 Using UniversalBiomeInjector (depth issues have been fixed)
            UniversalBiomeInjector.injectBiomes(consumer);

            LOGGER.info("🎉 Custom biome injection completed!");

        } catch (Exception e) {
            LOGGER.error("❌ Custom biome injection failed!", e);
            // Fallback: continue without custom biomes rather than crashing
            LOGGER.warn("Continuing world generation without custom biomes...");
        }
    }
}