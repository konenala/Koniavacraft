package com.github.nalamodikk.common.datagen.recipe.material;

import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableRecipe;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.recipe.ManaCraftingRecipeBuilder;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManaCraftingRecipeProvider {
    public static ManaCraftingTableRecipe shaped(ResourceLocation id, List<String> pattern, Map<String, Ingredient> stringKey, ItemStack result, int manaCost) {
        // 把 String 轉成 Character
        Map<Character, Ingredient> charKey = new HashMap<>();
        for (Map.Entry<String, Ingredient> entry : stringKey.entrySet()) {
            String keyStr = entry.getKey();
            if (keyStr.length() != 1) {
                throw new IllegalArgumentException("Key must be single character, got: '" + keyStr + "'");
            }
            charKey.put(keyStr.charAt(0), entry.getValue());
        }

        return ManaCraftingTableRecipe.createShaped(pattern, charKey, result, manaCost).withId(id);
    }


    public static void generate(RecipeOutput output) {
        // ✅ 無序合成配方
        ManaCraftingRecipeBuilder.create(ModItems.MANA_DUST.get(), 1)
                .addIngredient(Ingredient.of(Items.DIAMOND))
                .manaCost(5000)
                .save(output);


            ManaCraftingRecipeBuilder.create(ModBlocks.MANA_INFUSER.get(), 1)
                    .shaped(true)  // 明確設置為有序
                    .pattern("RGR")
                    .pattern("MIM")
                    .pattern("CDC")
                    .define('R', Items.REDSTONE_BLOCK)
                    .define('G', Items.GLASS)
                    .define('M', ModItems.MANA_DUST.get())
                    .define('I', Items.IRON_BLOCK)
                    .define('C', ModItems.REFINED_MANA_DUST.get())
                    .define('D', Items.DIAMOND)
                    .manaCost(50000)
                    .save(output, "mana_infuser_machine");

        // === ⚡ 升級模組配方 - 魔力合成台專用 ===
        
        // 📊 診斷顯示器升級模組 (入門級 - 8,000 魔力)
        ManaCraftingRecipeBuilder.create(ModItems.DIAGNOSTIC_DISPLAY_UPGRADE.get(), 1)
                .shaped(true)
                .pattern("GMG")
                .pattern("MCM")
                .pattern("GMG")
                .define('G', Items.GLASS)
                .define('M', ModItems.MANA_DUST.get())
                .define('C', Items.COMPARATOR)
                .manaCost(8000)
                .save(output, "diagnostic_display_upgrade_mana_crafting");

        // 🛢️ 擴展燃料倉升級模組 (初級 - 15,000 魔力)
        ManaCraftingRecipeBuilder.create(ModItems.EXPANDED_FUEL_CHAMBER_UPGRADE.get(), 1)
                .shaped(true)
                .pattern("IMI")
                .pattern("MCM")
                .pattern("IMI")
                .define('I', Items.IRON_BLOCK)
                .define('M', ModItems.MANA_DUST.get())
                .define('C', Items.CHEST)
                .manaCost(15000)
                .save(output, "expanded_fuel_chamber_upgrade_mana_crafting");

        // 🚀 加速處理升級模組 (中級 - 25,000 魔力)
        ManaCraftingRecipeBuilder.create(ModItems.ACCELERATED_PROCESSING_UPGRADE.get(), 1)
                .shaped(true)
                .pattern("EME")
                .pattern("MRM")
                .pattern("EME")
                .define('E', Items.EMERALD)
                .define('M', ModItems.MANA_DUST.get())
                .define('R', Items.REDSTONE_BLOCK)
                .manaCost(25000)
                .save(output, "accelerated_processing_upgrade_mana_crafting");

        // ⚗️ 催化轉換器升級模組 (高級 - 40,000 魔力)
        ManaCraftingRecipeBuilder.create(ModItems.CATALYTIC_CONVERTER_UPGRADE.get(), 1)
                .shaped(true)
                .pattern("NMN")
                .pattern("MBM")
                .pattern("NMN")
                .define('N', Items.NETHERITE_SCRAP)
                .define('M', ModItems.MANA_INGOT.get())
                .define('B', Items.BLAZE_ROD)
                .manaCost(40000)
                .save(output, "catalytic_converter_upgrade_mana_crafting");


        // ✅ 有序合成配方
//        ManaCraftingRecipeBuilder.create(ModItems.MANA_STAFF.get(), 1)
//                .pattern(" A ")
//                .pattern(" B ")
//                .pattern(" C ")
//                .define('A', Items.IRON_INGOT)
//                .define('B', Items.STICK)
//                .define('C', Items.DIAMOND)
//                .manaCost(2000)
//                .save(output);

    }

}
