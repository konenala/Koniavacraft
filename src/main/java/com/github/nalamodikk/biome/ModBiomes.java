package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * 模組生物群落註冊類 - 純數據包版本
 */
public class ModBiomes {

    // 📍 只保留 ResourceKey 定義
    public static final ResourceKey<Biome> MANA_PLAINS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_plains")
    );

    /**
     * 創建魔力草原生物群落（安全版本 - 避免特徵循環衝突）
     */
    public static Biome createManaPlains(HolderGetter<PlacedFeature> placedFeatures,
                                         HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        KoniavacraftMod.LOGGER.info("🌍 正在創建魔力草原生物群落（安全版本）...");

        // 使用帶參數的 Builder
        BiomeGenerationSettings.Builder generationSettings = new BiomeGenerationSettings.Builder(placedFeatures, carvers);
        MobSpawnSettings.Builder mobSpawnSettings = new MobSpawnSettings.Builder();

        // === 只添加安全的、不會造成循環的特徵 ===

        // 基礎地形特徵（安全）
        try {
            BiomeDefaultFeatures.addDefaultMonsterRoom(generationSettings);
            BiomeDefaultFeatures.addDefaultSprings(generationSettings);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.warn("跳過部分地形特徵以避免循環衝突");
        }

        // 礦石生成（通常安全）
        try {
            BiomeDefaultFeatures.addDefaultOres(generationSettings);
            BiomeDefaultFeatures.addDefaultSoftDisks(generationSettings);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.warn("跳過礦石特徵以避免循環衝突");
        }

        // 植被特徵（最安全的部分）
        try {
            BiomeDefaultFeatures.addDefaultGrass(generationSettings);
            BiomeDefaultFeatures.addPlainGrass(generationSettings);
            BiomeDefaultFeatures.addDefaultMushrooms(generationSettings);
        } catch (Exception e) {
            KoniavacraftMod.LOGGER.warn("跳過植被特徵以避免循環衝突");
        }

        // === 生物生成（這部分通常不會有問題）===
        // 被動生物
        mobSpawnSettings.addSpawn(MobCategory.CREATURE,
                new MobSpawnSettings.SpawnerData(EntityType.COW, 8, 4, 4));
        mobSpawnSettings.addSpawn(MobCategory.CREATURE,
                new MobSpawnSettings.SpawnerData(EntityType.PIG, 10, 4, 4));
        mobSpawnSettings.addSpawn(MobCategory.CREATURE,
                new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 12, 4, 4));
        mobSpawnSettings.addSpawn(MobCategory.CREATURE,
                new MobSpawnSettings.SpawnerData(EntityType.CHICKEN, 10, 4, 4));

        // 敵對生物
        BiomeDefaultFeatures.commonSpawns(mobSpawnSettings);

        // === 創建生物群落 ===
        Biome result = new Biome.BiomeBuilder()
                .temperature(0.7f)                    // 溫暖
                .downfall(0.6f)                       // 中等降雨
                .hasPrecipitation(true)               // 有降水
                .temperatureAdjustment(Biome.TemperatureModifier.NONE)
                .specialEffects(createEnhancedManaPlainsBiomeEffects())
                .mobSpawnSettings(mobSpawnSettings.build())
                .generationSettings(generationSettings.build())
                .build();

        KoniavacraftMod.LOGGER.info("✅ 魔力草原生物群落創建成功！");
        return result;
    }

    /**
     * 創建增強的魔力草原視覺效果
     */
    private static BiomeSpecialEffects createEnhancedManaPlainsBiomeEffects() {
        return new BiomeSpecialEffects.Builder()
                .waterColor(0x4A90E2)           // 明亮的魔力藍水
                .waterFogColor(0x2E5BDA)        // 深邃的藍色水霧
                .skyColor(0x87CEEB)             // 天空藍，比原版更亮
                .fogColor(0xE6E6FA)             // 淡薰衣草色霧氣
                .grassColorOverride(0x40E0D0)   // 青綠色魔力草 ✨
                .foliageColorOverride(0x20B2AA) // 海綠色葉子 ✨
                // 🎵 環境音效
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_MEADOW))
                // 🔊 TODO: 未來可以添加自定義音效
                // .ambientLoopSound(ModSounds.MANA_PLAINS_AMBIENT.get())
                // .ambientParticle(...) // 魔力粒子效果
                .build();
    }

    // === 未來的生物群落 ===

    // 水晶森林 (示例)
    public static final ResourceKey<Biome> CRYSTAL_FOREST = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "crystal_forest")
    );

    // 虛空之地 (示例)
    public static final ResourceKey<Biome> VOIDLANDS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "voidlands")
    );
}