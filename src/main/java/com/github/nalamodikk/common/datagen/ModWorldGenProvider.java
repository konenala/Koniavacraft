/**
 * 🌍 ModWorldGenProvider
 *
 * 此類為資料生成中的「世界生成註冊器」，負責將你自訂的礦物、特徵、地形修飾註冊進 Datapack Registry。
 * 它會輸出 `worldgen/configured_feature/`、`worldgen/placed_feature/` 等對應 JSON。
 *
 * ✅ 已註冊的項目：
 * - `ModConfiguredFeatures`：定義礦物/樹木的特徵結構（大小、樣式）。
 * - `ModPlacedFeatures`：定義這些特徵出現的位置與頻率（如層數、生物群系）。
 * - `ModBiomeModifiers`：控制特定生物群系中注入哪些特徵（可套用全域礦脈生成）。
 *
 * 📝 TODO：後續可能要補齊
 * - [ ] `ModDimensions::bootstrapType`：若未來要新增自訂維度。
 * - [ ] `ModBiomes::bootstrap`：如要新增自訂生物群系。
 * - [ ] `ModDimensions::bootstrapStem`：必要時補上維度樹（LevelStem）資訊。
 *
 * ⚠️ 注意：
 * 若不註冊於 `GatherDataEvent`，此類將不會被自動執行資料生成。
 */

package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.MagicalIndustryMod;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder();
//         //   .add(Registries.DIMENSION_TYPE, ModDimensions::bootstrapType)
//            .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap)
//            .add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap)
//            .add(ForgeRegistries.Keys.BIOME_MODIFIERS, ModBiomeModifiers::bootstrap);
//       /*     .add(Registries.BIOME, ModBiomes::boostrap)
//            .add(Registries.LEVEL_STEM, ModDimensions::bootstrapStem);
//
//        */

    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(MagicalIndustryMod.MOD_ID));
    }
}
