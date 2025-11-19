package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.coreapi.recipe.ProcessingRecipe;
import com.github.nalamodikk.register.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ⚙️ 加工配方數據生成器
 *
 * 此類自動生成所有加工配方的 JSON 文件
 * 執行 ./gradlew runData 時自動運行
 * 輸出到 src/generated/resources/data/koniava/recipes/
 */
public class ProcessingRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ProcessingRecipeProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        super(pOutput, pRegistries);
    }

    @Override
    protected void buildRecipes(RecipeOutput pRecipeOutput) {
        // ============================================
        // 🔨 粉碎機配方（Grinder Recipes）
        // ============================================

        // 🪨 石頭 → 沙粒
        createProcessingRecipe(pRecipeOutput, "grinder/stone_grind")
                .inputs(Ingredient.of(Blocks.STONE))
                .mainOutput(new ItemStack(Items.SAND, 1))
                .manaCost(50)
                .processingTime(100)
                .machineType("grinder")
                .save();

        // 💎 鑽石礦 → 鑽石粉（假設有此物品）
        createProcessingRecipe(pRecipeOutput, "grinder/diamond_ore_grind")
                .inputs(Ingredient.of(Items.DIAMOND))
                .mainOutput(new ItemStack(Items.GLASS, 1))  // 臨時用玻璃代替
                .addChanceOutput(new ItemStack(Items.GRAVEL, 1), 0.2f)  // 20% 副產品
                .manaCost(200)
                .processingTime(150)
                .machineType("grinder")
                .save();

        // ============================================
        // 🌊 清洗機配方（Washer Recipes）
        // ============================================

        // 灰塵 → 淨化物品
        createProcessingRecipe(pRecipeOutput, "washer/dust_clean")
                .inputs(Ingredient.of(Items.GRAVEL))
                .mainOutput(new ItemStack(Items.SAND, 1))
                .manaCost(75)
                .processingTime(80)
                .machineType("washer")
                .save();

        // ============================================
        // ✨ 富集機配方（Enricher Recipes）
        // ============================================

        // 沙粒 → 濃縮物
        createProcessingRecipe(pRecipeOutput, "enricher/sand_enrich")
                .inputs(Ingredient.of(Items.SAND))
                .mainOutput(new ItemStack(Items.DIRT, 1))  // 臨時示例
                .manaCost(100)
                .processingTime(120)
                .machineType("enricher")
                .save();

        // ============================================
        // 🔨 多輸入示例
        // ============================================

        // 石頭 + 圓石 → 磚塊（示例多輸入）
        createProcessingRecipe(pRecipeOutput, "grinder/multi_input_example")
                .inputs(
                        Ingredient.of(Blocks.STONE),
                        Ingredient.of(Blocks.COBBLESTONE)
                )
                .mainOutput(new ItemStack(Items.BRICKS, 2))
                .addChanceOutput(new ItemStack(Items.CLAY_BALL, 1), 0.15f)
                .manaCost(150)
                .processingTime(200)
                .machineType("grinder")
                .save();

        KoniavacraftMod.LOGGER.info("✅ 生成了 {} 個加工配方", 5);
    }

    /**
     * 🔧 配方構建器（流暢 API）
     */
    private ProcessingRecipeBuilder createProcessingRecipe(RecipeOutput output, String name) {
        return new ProcessingRecipeBuilder(output, name);
    }

    /**
     * 🔨 內部構建器類
     */
    public static class ProcessingRecipeBuilder {
        private final RecipeOutput output;
        private final String name;
        private final List<Ingredient> inputs = new ArrayList<>();
        private ItemStack mainOutput = ItemStack.EMPTY;
        private final List<ProcessingRecipe.ChanceOutput> chanceOutputs = new ArrayList<>();
        private int manaCost = 0;
        private int processingTime = 200;
        private String machineType = "grinder";

        public ProcessingRecipeBuilder(RecipeOutput output, String name) {
            this.output = output;
            this.name = name;
        }

        public ProcessingRecipeBuilder inputs(Ingredient... ingredients) {
            for (Ingredient ingredient : ingredients) {
                this.inputs.add(ingredient);
            }
            return this;
        }

        public ProcessingRecipeBuilder mainOutput(ItemStack output) {
            this.mainOutput = output;
            return this;
        }

        public ProcessingRecipeBuilder addChanceOutput(ItemStack output, float chance) {
            this.chanceOutputs.add(new ProcessingRecipe.ChanceOutput(output, chance));
            return this;
        }

        public ProcessingRecipeBuilder manaCost(int cost) {
            this.manaCost = cost;
            return this;
        }

        public ProcessingRecipeBuilder processingTime(int ticks) {
            this.processingTime = ticks;
            return this;
        }

        public ProcessingRecipeBuilder machineType(String type) {
            this.machineType = type;
            return this;
        }

        /**
         * 儲存配方到 JSON
         */
        public void save() {
            // 建立 NonNullList
            net.minecraft.core.NonNullList<Ingredient> ingredientList = net.minecraft.core.NonNullList.create();
            ingredientList.addAll(inputs);

            // 建立配方物件
            ProcessingRecipe recipe = new ProcessingRecipe(
                    ingredientList,
                    mainOutput,
                    chanceOutputs,
                    manaCost,
                    processingTime,
                    machineType
            );

            // 建立配方ID
            ResourceLocation recipeId =
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, name);

            // 輸出到 JSON（暫不包含advancement）
            this.output.accept(recipeId, recipe, null);
        }
    }
}
