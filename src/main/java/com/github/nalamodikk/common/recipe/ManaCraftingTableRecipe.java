package com.github.nalamodikk.common.recipe;

import com.github.nalamodikk.common.register.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Function5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.*;
public record ManaCraftingTableRecipe(ResourceLocation id,NonNullList<Ingredient> ingredients,ItemStack result,int manaCost,boolean isShaped) implements Recipe<ManaCraftingTableRecipe.ManaCraftingInput>{

    @Override
    public boolean matches(ManaCraftingInput input, Level level) {
        if (level.isClientSide()) {
            return false;
        }

        if (input.getContainerSize() != 9) {
            return false;
        }

        if (isShaped) {
            // 有序合成
            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient expected = ingredients.get(i);
                ItemStack actual = input.getItem(i);
                if (!expected.test(actual)) {
                    return false;
                }
            }
            return true;
        } else {
            // 無序合成
            List<Ingredient> remainingIngredients = new ArrayList<>(ingredients);
            int usedSlotCount = 0;

            for (int i = 0; i < 9; i++) {
                ItemStack stackInSlot = input.getItem(i);
                if (stackInSlot.isEmpty()) {
                    continue;
                }

                usedSlotCount++;

                boolean matched = false;
                Iterator<Ingredient> iterator = remainingIngredients.iterator();
                while (iterator.hasNext()) {
                    Ingredient ingredient = iterator.next();
                    if (ingredient.test(stackInSlot)) {
                        iterator.remove();
                        matched = true;
                        break;
                    }
                }

                if (!matched) {
                    return false;
                }
            }

            return remainingIngredients.isEmpty() && usedSlotCount == ingredients.size();
        }
    }

    @Override
    public ItemStack assemble(ManaCraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return isShaped ? (width == 3 && height == 3) : (width * height >= ingredients.size());
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }



    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MANA_CRAFTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MANA_CRAFTING_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    public int getManaCost() {
        return manaCost;
    }

    public boolean isShaped() {
        return isShaped;
    }
    @Override
    public boolean isSpecial() {
        return true;
    }



    public static class Serializer implements RecipeSerializer<ManaCraftingTableRecipe> {
        public static final MapCodec<ManaCraftingTableRecipe> CODEC = RecordCodecBuilder.mapCodec(inst ->
                inst.group(
                        ResourceLocation.CODEC.fieldOf("id").forGetter(ManaCraftingTableRecipe::id),
                        Ingredient.CODEC.listOf().xmap(
                                list -> NonNullList.of(Ingredient.EMPTY, list.toArray(Ingredient[]::new)),
                                list -> List.copyOf(list)
                        ).fieldOf("ingredients").forGetter(ManaCraftingTableRecipe::ingredients),
                        ItemStack.CODEC.fieldOf("result").forGetter(ManaCraftingTableRecipe::result),
                        Codec.INT.fieldOf("mana_cost").forGetter(ManaCraftingTableRecipe::manaCost),
                        Codec.BOOL.fieldOf("shaped").forGetter(ManaCraftingTableRecipe::isShaped)
                ).apply(inst, ManaCraftingTableRecipe::new)
        );



        public static final StreamCodec<RegistryFriendlyByteBuf, List<Ingredient>> INGREDIENT_LIST_STREAM_CODEC =
                StreamCodec.of(
                        (buf, list) -> {
                            buf.writeVarInt(list.size());
                            for (Ingredient ingredient : list) {
                                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                            }
                        },
                        buf -> {
                            int size = buf.readVarInt();
                            List<Ingredient> list = new ArrayList<>(size);
                            for (int i = 0; i < size; i++) {
                                list.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                            }
                            return list;
                        }
                );


        @SuppressWarnings("unchecked")
        public static final StreamCodec<RegistryFriendlyByteBuf, ManaCraftingTableRecipe> STREAM_CODEC =
                (StreamCodec<RegistryFriendlyByteBuf, ManaCraftingTableRecipe>) StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC.mapStream(buf -> buf),
                        ManaCraftingTableRecipe::id,

                        INGREDIENT_LIST_STREAM_CODEC,
                        ManaCraftingTableRecipe::ingredients,

                        ItemStack.STREAM_CODEC,
                        ManaCraftingTableRecipe::result,

                        ByteBufCodecs.VAR_INT, // ✅ 換掉 Codec.INT
                        ManaCraftingTableRecipe::manaCost,

                        ByteBufCodecs.BOOL,    // ✅ 換掉 Codec.BOOL
                        ManaCraftingTableRecipe::isShaped,

                        (id, ingredients, result, mana, shaped) -> new ManaCraftingTableRecipe(
                                id,
                                ingredients.isEmpty()
                                        ? NonNullList.create()
                                        : NonNullList.of(Ingredient.EMPTY, ingredients.toArray(new Ingredient[0])),
                                result.copy(),
                                mana,
                                shaped
                        )
                );






        @Override
        public MapCodec<ManaCraftingTableRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ManaCraftingTableRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Type implements RecipeType<ManaCraftingTableRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mana_crafting";

        @Override
        public String toString() {
            return ID;
        }
    }
    public static class ManaCraftingInput extends SimpleContainer implements RecipeInput {
        public ManaCraftingInput(int size) {
            super(size);
        }

        @Override
        public int size() {
            return super.getContainerSize();
        }

    }


}
