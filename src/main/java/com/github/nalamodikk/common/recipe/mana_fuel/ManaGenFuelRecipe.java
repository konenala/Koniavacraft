package com.github.nalamodikk.common.recipe.mana_fuel;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;

public record ManaGenFuelRecipe(ResourceLocation id,String itemId,int manaRate,int energyRate,int burnTime)  implements Recipe<RecipeInput> {

    public ResourceLocation getItemResource() {
        return ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID,this.itemId);
    }

    @Override
    public boolean matches(RecipeInput container, Level level) {
        for (int i = 0; i < container.size(); i++) {
            ItemStack itemStack = container.getItem(i);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            if (itemId != null && itemId.toString().equals(this.itemId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput container, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FuelRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return FuelRecipeType.INSTANCE;
    }

    public static class FuelRecipeSerializer implements RecipeSerializer<ManaGenFuelRecipe> {
        public static final FuelRecipeSerializer INSTANCE = new FuelRecipeSerializer();

        public static final MapCodec<ManaGenFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                        ResourceLocation.CODEC.fieldOf("id").forGetter(ManaGenFuelRecipe::id),
                        Codec.STRING.fieldOf("item").forGetter(ManaGenFuelRecipe::itemId),
                        Codec.INT.fieldOf("mana").forGetter(ManaGenFuelRecipe::manaRate),
                        Codec.INT.fieldOf("energy").forGetter(ManaGenFuelRecipe::energyRate),
                        Codec.INT.optionalFieldOf("burn_time", 200).forGetter(ManaGenFuelRecipe::burnTime)
                ).apply(inst, ManaGenFuelRecipe::new)
        );

        public static final StreamCodec<FriendlyByteBuf, ManaGenFuelRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, ManaGenFuelRecipe::id,
                        ByteBufCodecs.STRING_UTF8, ManaGenFuelRecipe::itemId,
                        ByteBufCodecs.INT, ManaGenFuelRecipe::manaRate,
                        ByteBufCodecs.INT, ManaGenFuelRecipe::energyRate,
                        ByteBufCodecs.INT, ManaGenFuelRecipe::burnTime,
                        ManaGenFuelRecipe::new
                );

        @Override
        public MapCodec<ManaGenFuelRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<FriendlyByteBuf, ?> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class FuelRecipeType implements RecipeType<ManaGenFuelRecipe> {
        public static final FuelRecipeType INSTANCE = new FuelRecipeType();
        private FuelRecipeType() {}
    }
}
