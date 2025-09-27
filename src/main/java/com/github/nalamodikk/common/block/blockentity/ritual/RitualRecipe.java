package com.github.nalamodikk.common.block.blockentity.ritual;

import com.github.nalamodikk.register.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * 儀式配方 - 定義儀式的所有要求和產出
 */
public class RitualRecipe implements Recipe<RitualRecipe.RitualInput> {
    
    private final ResourceLocation id;
    private final String name;
    private final RitualTier tier;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final List<ItemStack> additionalResults;
    private final int manaCost;
    private final int ritualTime;
    private final float failureChance;
    private final Map<String, Integer> structureRequirements;
    
    public RitualRecipe(ResourceLocation id, String name, RitualTier tier,
                       NonNullList<Ingredient> ingredients, ItemStack result,
                       List<ItemStack> additionalResults, int manaCost,
                       int ritualTime, float failureChance,
                       Map<String, Integer> structureRequirements) {
        this.id = id;
        this.name = name;
        this.tier = tier;
        this.ingredients = ingredients;
        this.result = result;
        this.additionalResults = additionalResults;
        this.manaCost = manaCost;
        this.ritualTime = ritualTime;
        this.failureChance = failureChance;
        this.structureRequirements = structureRequirements;
    }

    @Override
    public boolean matches(RitualInput input, Level level) {
        if (!matchesIngredientsAndMana(input)) {
            return false;
        }
        return findFirstUnmetStructureRequirement(input.getAvailableStructure()).isEmpty();
    }

    /**
     * 檢查祭品與魔力是否符合要求（不檢查結構）。
     */
    public boolean matchesIngredientsAndMana(RitualInput input) {
        List<ItemStack> availableItems = input.getIngredients();
        if (availableItems.size() < ingredients.size()) {
            return false;
        }

        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (ItemStack available : availableItems) {
                if (ingredient.test(available)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return input.getAvailableMana() >= manaCost;
    }

    /**
     * 找出第一個未達標的結構需求。
     */
    public Optional<StructureRequirementStatus> findFirstUnmetStructureRequirement(Map<String, Integer> availableStructure) {
        if (structureRequirements.isEmpty()) {
            return Optional.empty();
        }
        for (Map.Entry<String, Integer> entry : structureRequirements.entrySet()) {
            int actual = availableStructure.getOrDefault(entry.getKey(), 0);
            if (actual < entry.getValue()) {
                return Optional.of(new StructureRequirementStatus(entry.getKey(), entry.getValue(), actual));
            }
        }
        return Optional.empty();
    }

    @Override
    public ItemStack assemble(RitualInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true; // 儀式不受尺寸限制
    }

    public record StructureRequirementStatus(String key, int required, int actual) {}

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RITUAL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.RITUAL_TYPE.get();
    }

    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    // Getters
    public String getName() { return name; }
    public RitualTier getTier() { return tier; }
    public List<ItemStack> getAdditionalResults() { return additionalResults; }
    public int getManaCost() { return manaCost; }
    public int getRitualTime() { return ritualTime; }
    public float getFailureChance() { return failureChance; }
    public Map<String, Integer> getStructureRequirements() { return structureRequirements; }

    /**
     * 儀式輸入容器
     */
    public static class RitualInput implements RecipeInput {
        private final List<ItemStack> ingredients;
        private final int availableMana;
        private final Map<String, Integer> availableStructure;
        
        public RitualInput(List<ItemStack> ingredients, int availableMana, 
                          Map<String, Integer> availableStructure) {
            this.ingredients = ingredients;
            this.availableMana = availableMana;
            this.availableStructure = availableStructure;
        }

        @Override
        public ItemStack getItem(int index) {
            return index < ingredients.size() ? ingredients.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return ingredients.size();
        }
        
        public List<ItemStack> getIngredients() { return ingredients; }
        public int getAvailableMana() { return availableMana; }
        public Map<String, Integer> getAvailableStructure() { return availableStructure; }
    }

    /**
     * 儀式等級枚舉
     */
    public enum RitualTier {
        BASIC(1, "基礎"),
        INTERMEDIATE(2, "中級"),
        ADVANCED(3, "高級"),
        MASTER(4, "大師"),
        FORBIDDEN(5, "禁忌");

        public final int level;
        public final String displayName;

        RitualTier(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
    }

    /**
     * 儀式配方序列化器
     */
    public static class Serializer implements RecipeSerializer<RitualRecipe> {

        private static final MapCodec<RitualRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(RitualRecipe::getId),
                Codec.STRING.fieldOf("name").forGetter(RitualRecipe::getName),
                Codec.STRING.xmap(
                    name -> RitualTier.valueOf(name.toUpperCase()),
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
                NonNullList<Ingredient> nonNullIngredients = NonNullList.create();
                nonNullIngredients.addAll(ingredients);
                return new RitualRecipe(id, name, tier, nonNullIngredients, result, additionalResults, manaCost, ritualTime, failureChance, structureRequirements);
            })
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> STREAM_CODEC = StreamCodec.of(
            Serializer::toNetwork,
            Serializer::fromNetwork
        );

        @Override
        public MapCodec<RitualRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, RitualRecipe recipe) {
            buffer.writeResourceLocation(recipe.getId());
            buffer.writeUtf(recipe.getName());
            buffer.writeEnum(recipe.getTier());

            buffer.writeVarInt(recipe.getIngredients().size());
            for (Ingredient ingredient : recipe.getIngredients()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }

            ItemStack.STREAM_CODEC.encode(buffer, recipe.getResultItem(null));

            buffer.writeVarInt(recipe.getAdditionalResults().size());
            for (ItemStack stack : recipe.getAdditionalResults()) {
                ItemStack.STREAM_CODEC.encode(buffer, stack);
            }

            buffer.writeVarInt(recipe.getManaCost());
            buffer.writeVarInt(recipe.getRitualTime());
            buffer.writeFloat(recipe.getFailureChance());

            Map<String, Integer> structure = recipe.getStructureRequirements();
            buffer.writeVarInt(structure.size());
            for (Map.Entry<String, Integer> entry : structure.entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeVarInt(entry.getValue());
            }
        }

        private static RitualRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            ResourceLocation id = buffer.readResourceLocation();
            String name = buffer.readUtf();
            RitualTier tier = buffer.readEnum(RitualTier.class);

            int ingredientCount = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }

            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);

            int additionalCount = buffer.readVarInt();
            List<ItemStack> additionalResults = new java.util.ArrayList<>();
            for (int i = 0; i < additionalCount; i++) {
                additionalResults.add(ItemStack.STREAM_CODEC.decode(buffer));
            }

            int manaCost = buffer.readVarInt();
            int ritualTime = buffer.readVarInt();
            float failureChance = buffer.readFloat();

            int structureCount = buffer.readVarInt();
            Map<String, Integer> structureRequirements = new HashMap<>();
            for (int i = 0; i < structureCount; i++) {
                String key = buffer.readUtf();
                int value = buffer.readVarInt();
                structureRequirements.put(key, value);
            }

            return new RitualRecipe(id, name, tier, ingredients, result, additionalResults,
                                  manaCost, ritualTime, failureChance, structureRequirements);
        }
    }
}