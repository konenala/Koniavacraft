package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用生物群落注入器
 * 支持注册多個生物群落和靈活的氣候配置
 */
public class UniversalBiomeInjector {

    // 存储所有需要注入的生物群落
    private static final List<BiomeEntry> CUSTOM_BIOMES = new ArrayList<>();

    /**
     * 生物群落條目
     */
    public static class BiomeEntry {
        public final ResourceKey<Biome> biome;
        public final ClimateConfig climate;
        public final int weight; // 生成權重 (1-10)
        public final String description;

        public BiomeEntry(ResourceKey<Biome> biome, ClimateConfig climate, int weight, String description) {
            this.biome = biome;
            this.climate = climate;
            this.weight = Math.max(1, Math.min(10, weight)); // 限制在 1-10 之間
            this.description = description;
        }
    }

    /**
     * 氣候配置類
     */
    public static class ClimateConfig {
        public final Climate.Parameter temperature;
        public final Climate.Parameter humidity;
        public final Climate.Parameter continentalness;
        public final Climate.Parameter erosion;
        public final Climate.Parameter depth;
        public final Climate.Parameter weirdness;
        public final float offset;

        private ClimateConfig(Climate.Parameter temperature, Climate.Parameter humidity,
                              Climate.Parameter continentalness, Climate.Parameter erosion,
                              Climate.Parameter depth, Climate.Parameter weirdness, float offset) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.depth = depth;
            this.weirdness = weirdness;
            this.offset = offset;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Climate.Parameter temperature = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter humidity = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter continentalness = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter erosion = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter depth = Climate.Parameter.span(-2.0F, 2.0F);
            private Climate.Parameter weirdness = Climate.Parameter.span(-2.0F, 2.0F);
            private float offset = 0.0F;

            public Builder temperature(float min, float max) {
                this.temperature = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder humidity(float min, float max) {
                this.humidity = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder continentalness(float min, float max) {
                this.continentalness = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder erosion(float min, float max) {
                this.erosion = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder depth(float min, float max) {
                this.depth = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder weirdness(float min, float max) {
                this.weirdness = Climate.Parameter.span(min, max);
                return this;
            }

            public Builder offset(float offset) {
                this.offset = offset;
                return this;
            }

            public ClimateConfig build() {
                return new ClimateConfig(temperature, humidity, continentalness, erosion, depth, weirdness, offset);
            }
        }
    }

    /**
     * 預設氣候配置
     */
    public static class ClimatePresets {
        // 溫帶草原 (類似原版平原)
        public static final ClimateConfig TEMPERATE_PLAINS = ClimateConfig.builder()
                .temperature(0.6F, 0.8F)
                .humidity(0.4F, 0.7F)
                .continentalness(-0.1F, 0.3F)
                .erosion(-0.2F, 0.2F)
                .depth(0.0F, 0.4F)           // 🌟 關鍵：地面到小丘陵高度
                .weirdness(-0.5F, 0.5F)
                .build();

        // 神秘森林
        public static final ClimateConfig MYSTICAL_FOREST = ClimateConfig.builder()
                .temperature(0.4F, 0.7F)
                .humidity(0.6F, 1.0F)
                .continentalness(0.1F, 0.5F)
                .erosion(-0.3F, 0.1F)
                .depth(-0.2F, 0.2F)
                .weirdness(0.3F, 1.0F)
                .build();

        // 寒冷高原
        public static final ClimateConfig COLD_HIGHLANDS = ClimateConfig.builder()
                .temperature(-0.5F, 0.2F)
                .humidity(0.2F, 0.6F)
                .continentalness(0.3F, 0.8F)
                .erosion(-0.1F, 0.3F)
                .depth(0.2F, 0.8F)
                .weirdness(-0.3F, 0.3F)
                .build();

        // 沙漠綠洲
        public static final ClimateConfig DESERT_OASIS = ClimateConfig.builder()
                .temperature(0.8F, 1.2F)
                .humidity(0.1F, 0.4F)
                .continentalness(-0.2F, 0.2F)
                .erosion(-0.4F, -0.1F)
                .depth(-0.3F, 0.0F)
                .weirdness(0.1F, 0.6F)
                .build();
    }

    /**
     * 注册生物群落
     */
    public static void registerBiome(ResourceKey<Biome> biome, ClimateConfig climate, int weight, String description) {
        CUSTOM_BIOMES.add(new BiomeEntry(biome, climate, weight, description));
        KoniavacraftMod.LOGGER.info("📝 註冊生物群落: {} (權重: {}) - {}",
                biome.location(), weight, description);
    }

    /**
     * 注册生物群落 (簡化版，使用預設權重)
     */
    public static void registerBiome(ResourceKey<Biome> biome, ClimateConfig climate, String description) {
        registerBiome(biome, climate, 5, description);
    }

    /**
     * 注入所有已註冊的生物群落
     */
    public static void injectBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer) {
        KoniavacraftMod.LOGGER.info("🌍 UniversalBiomeInjector: 開始注入 {} 個自訂生物群落...", CUSTOM_BIOMES.size());

        int successCount = 0;
        for (BiomeEntry entry : CUSTOM_BIOMES) {
            try {
                injectSingleBiome(consumer, entry);
                successCount++;
                KoniavacraftMod.LOGGER.debug("✅ 成功注入: {}", entry.biome.location());
            } catch (Exception e) {
                KoniavacraftMod.LOGGER.error("❌ 注入失敗: {} - {}", entry.biome.location(), e.getMessage());
            }
        }

        KoniavacraftMod.LOGGER.info("🎉 生物群落注入完成！成功: {}/{}", successCount, CUSTOM_BIOMES.size());
    }

    /**
     * 注入單個生物群落
     */
    private static void injectSingleBiome(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> consumer, BiomeEntry entry) {
        ClimateConfig climate = entry.climate;

        // 根據權重決定注入次數 (權重越高，出現機會越大)
        int injectionCount = Math.max(1, entry.weight / 3);

        for (int i = 0; i < injectionCount; i++) {
            // 為每次注入添加小幅隨機偏移，增加自然感
            long randomOffset = (long)((i * 0.1F) - 0.05F) * 1000L; // 轉換為 long

            // 修正：使用正確的 ParameterPoint 構造方法，最後一個參數是 long
            Climate.ParameterPoint point = new Climate.ParameterPoint(
                    climate.temperature,
                    climate.humidity,
                    climate.continentalness,
                    climate.erosion,
                    climate.depth,
                    climate.weirdness,

                    (long)(climate.offset * 1000L) + randomOffset // 轉換為 long
            );

            consumer.accept(Pair.of(point, entry.biome));
        }
    }

    /**
     * 獲取所有註冊的生物群落 (調試用)
     */
    public static List<BiomeEntry> getRegisteredBiomes() {
        return new ArrayList<>(CUSTOM_BIOMES);
    }

    /**
     * 清空所有註冊的生物群落 (測試用)
     */
    public static void clearAll() {
        CUSTOM_BIOMES.clear();
        KoniavacraftMod.LOGGER.info("🧹 清空所有自訂生物群落註冊");
    }
}