package com.github.nalamodikk.common.block.blockentity.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;
import java.util.Map;

/**
 * 儀式配方序列化器 - 處理儀式配方的序列化和反序列化
 */
public class RitualRecipeSerializer implements RecipeSerializer<RitualRecipe> {

    public static final MapCodec<RitualRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(RitualRecipe::getId),
            Codec.STRING.fieldOf("name").forGetter(RitualRecipe::getName),
            Codec.STRING.xmap(
                name -> RitualRecipe.RitualTier.valueOf(name.toUpperCase()),
                tier -> tier.name().toLowerCase()
            ).fieldOf("tier").forGetter(RitualRecipe::getTier),
            Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.getIngredients().stream().toList()),
            ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.getResultItem(null)),
            ItemStack.CODEC.listOf().optionalFieldOf("additional_results", List.of()).forGetter(RitualRecipe::getAdditionalResults),
            Codec.INT.fieldOf("mana_cost").forGetter(RitualRecipe::getManaCost),
            Codec.INT.fieldOf("ritual_time").forGetter(RitualRecipe::getRitualTime),
            Codec.FLOAT.optionalFieldOf("failure_chance", 0.0f).forGetter(RitualRecipe::getFailureChance),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("structure_requirements", Map.of()).forGetter(RitualRecipe::getStructureRequirements)
        ).apply(instance, (id, name, tier, ingredients, result, additionalResults, manaCost, ritualTime, failureChance, structureRequirements) -> {
            NonNullList<Ingredient> ingredientList = NonNullList.create();
            ingredientList.addAll(ingredients);
            return new RitualRecipe(id, name, tier, ingredientList, result, additionalResults, 
                                  manaCost, ritualTime, failureChance, structureRequirements);
        })
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> STREAM_CODEC = StreamCodec.of(
        RitualRecipeSerializer::toNetwork,
        RitualRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<RitualRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    public static RitualRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        ResourceLocation id = buffer.readResourceLocation();
        String name = buffer.readUtf();
        RitualRecipe.RitualTier tier = buffer.readEnum(RitualRecipe.RitualTier.class);
        
        int ingredientCount = buffer.readInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(ingredientCount, Ingredient.EMPTY);
        for (int i = 0; i < ingredientCount; i++) {
            ingredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
        }
        
        ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
        
        int additionalCount = buffer.readInt();
        List<ItemStack> additionalResults = new java.util.ArrayList<>();
        for (int i = 0; i < additionalCount; i++) {
            additionalResults.add(ItemStack.STREAM_CODEC.decode(buffer));
        }
        
        int manaCost = buffer.readInt();
        int ritualTime = buffer.readInt();
        float failureChance = buffer.readFloat();
        
        int structureCount = buffer.readInt();
        Map<String, Integer> structureRequirements = new java.util.HashMap<>();
        for (int i = 0; i < structureCount; i++) {
            String key = buffer.readUtf();
            int value = buffer.readInt();
            structureRequirements.put(key, value);
        }
        
        return new RitualRecipe(id, name, tier, ingredients, result, additionalResults,
                              manaCost, ritualTime, failureChance, structureRequirements);
    }

    public static void toNetwork(RegistryFriendlyByteBuf buffer, RitualRecipe recipe) {
        buffer.writeResourceLocation(recipe.getId());
        buffer.writeUtf(recipe.getName());
        buffer.writeEnum(recipe.getTier());
        
        buffer.writeInt(recipe.getIngredients().size());
        for (Ingredient ingredient : recipe.getIngredients()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
        }
        
        ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));
        
        buffer.writeInt(recipe.getAdditionalResults().size());
        for (ItemStack stack : recipe.getAdditionalResults()) {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
        }
        
        buffer.writeInt(recipe.getManaCost());
        buffer.writeInt(recipe.getRitualTime());
        buffer.writeFloat(recipe.getFailureChance());
        
        buffer.writeInt(recipe.getStructureRequirements().size());
        for (Map.Entry<String, Integer> entry : recipe.getStructureRequirements().entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeInt(entry.getValue());
        }
    }
}