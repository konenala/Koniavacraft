package com.github.nalamodikk.common.block.blockentity.mana_infuser;

import com.github.nalamodikk.register.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/**
 * 🔮 魔力注入配方
 *
 * 功能：
 * - 定義哪些物品可以被魔力注入
 * - 指定需要的魔力消耗
 * - 指定注入時間
 * - 定義注入後的結果物品
 */
public class ManaInfuserRecipe implements Recipe<ManaInfuserRecipe.ManaInfuserInput> {

    // === 📦 配方數據 ===
    private final Ingredient input;        // 輸入物品要求
    private final ItemStack result;        // 輸出結果
    private final int manaCost;           // 魔力消耗
    private final int infusionTime;       // 注入時間 (ticks)
    private final int inputCount;         // 需要的輸入物品數量

    public ManaInfuserRecipe(Ingredient input, ItemStack result, int manaCost, int infusionTime, int inputCount) {
        this.input = input;
        this.result = result;
        this.manaCost = manaCost;
        this.infusionTime = infusionTime;
        this.inputCount = inputCount;
    }

    // === 🔍 配方匹配邏輯 ===

    @Override
    public boolean matches(ManaInfuserInput input, Level level) {
        ItemStack inputStack = input.getInputStack();
        return this.input.test(inputStack) && inputStack.getCount() >= this.inputCount;
    }

    @Override
    public ItemStack assemble(ManaInfuserInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // 魔力注入機不受尺寸限制
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    // === 📊 配方屬性 ===

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getManaCost() {
        return manaCost;
    }

    public int getInfusionTime() {
        return infusionTime;
    }

    public int getInputCount() {
        return inputCount;
    }

    // === 🏷️ 配方元數據 ===

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MANA_INFUSER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MANA_INFUSER_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.input);
        return ingredients;
    }

    // === 📦 輸入容器類 ===

    /**
     * 🔧 魔力注入機的輸入容器
     */
    public static class ManaInfuserInput implements RecipeInput {
        private final ItemStack inputStack;

        public ManaInfuserInput(ItemStack inputStack) {
            this.inputStack = inputStack;
        }

        public ItemStack getInputStack() {
            return inputStack;
        }

        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? inputStack : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    // === 📝 序列化器 ===

    /**
     * 🔧 魔力注入配方序列化器
     */
    public static class Serializer implements RecipeSerializer<ManaInfuserRecipe> {

        // MapCodec 用於 JSON 序列化
        private static final MapCodec<ManaInfuserRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(recipe -> recipe.input),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                        Codec.INT.fieldOf("mana_cost").forGetter(recipe -> recipe.manaCost),
                        Codec.INT.optionalFieldOf("infusion_time", 60).forGetter(recipe -> recipe.infusionTime),
                        Codec.INT.optionalFieldOf("input_count", 1).forGetter(recipe -> recipe.inputCount)
                ).apply(instance, ManaInfuserRecipe::new)
        );

        // StreamCodec 用於網路序列化
        private static final StreamCodec<RegistryFriendlyByteBuf, ManaInfuserRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.input,
                        ItemStack.STREAM_CODEC, recipe -> recipe.result,
                        ByteBufCodecs.VAR_INT, recipe -> recipe.manaCost,
                        ByteBufCodecs.VAR_INT, recipe -> recipe.infusionTime,
                        ByteBufCodecs.VAR_INT, recipe -> recipe.inputCount,
                        ManaInfuserRecipe::new
                );

        @Override
        public MapCodec<ManaInfuserRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ManaInfuserRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}