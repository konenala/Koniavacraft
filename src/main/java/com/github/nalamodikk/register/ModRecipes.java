package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_crafting.ManaCraftingTableRecipe;
import com.github.nalamodikk.common.block.blockentity.mana_generator.recipe.ManaGenFuelRecipe;
import com.github.nalamodikk.common.block.blockentity.mana_infuser.ManaInfuserRecipe;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.RitualRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, KoniavacraftMod.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, KoniavacraftMod.MOD_ID);

    /**
     * 魔法合成台配方系統
     */
    private static final RecipeType<ManaCraftingTableRecipe> MANA_CRAFTING_TYPE_INSTANCE =
            new RecipeType<>() {
                @Override
                public String toString() {
                    return KoniavacraftMod.MOD_ID + ":mana_crafting";
                }
            };

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ManaCraftingTableRecipe>> MANA_CRAFTING_SERIALIZER =
            SERIALIZERS.register("mana_crafting", ManaCraftingTableRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ManaCraftingTableRecipe>> MANA_CRAFTING_TYPE =
            TYPES.register("mana_crafting", () -> MANA_CRAFTING_TYPE_INSTANCE);

    /**
     * 魔力注入機配方系統
     */
    public static final Supplier<RecipeType<ManaInfuserRecipe>> MANA_INFUSER_TYPE =
            TYPES.register("mana_infuser", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_infuser")));

    public static final Supplier<RecipeSerializer<ManaInfuserRecipe>> MANA_INFUSER_SERIALIZER =
            SERIALIZERS.register("mana_infuser", ManaInfuserRecipe.Serializer::new);

    /**
     * 魔力發電機燃料配方系統
     */
    private static final RecipeType<ManaGenFuelRecipe> MANA_FUEL_TYPE_INSTANCE = new RecipeType<>() {
        @Override
        public String toString() {
            return KoniavacraftMod.MOD_ID + ":mana_fuel";
        }
    };

    public static final DeferredHolder<RecipeType<?>, RecipeType<ManaGenFuelRecipe>> MANA_FUEL_TYPE =
            TYPES.register("mana_fuel", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return KoniavacraftMod.MOD_ID + ":mana_fuel";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ManaGenFuelRecipe>> MANA_FUEL_SERIALIZER =
            SERIALIZERS.register("mana_fuel", () -> ManaGenFuelRecipe.FuelRecipeSerializer.INSTANCE);

    /**
     * 儀式系統配方
     */
    private static final RecipeType<RitualRecipe> RITUAL_TYPE_INSTANCE = new RecipeType<>() {
        @Override
        public String toString() {
            return KoniavacraftMod.MOD_ID + ":ritual";
        }
    };

    public static final DeferredHolder<RecipeType<?>, RecipeType<RitualRecipe>> RITUAL_TYPE =
            TYPES.register("ritual", () -> RITUAL_TYPE_INSTANCE);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RitualRecipe>> RITUAL_SERIALIZER =
            SERIALIZERS.register("ritual", RitualRecipe.Serializer::new);

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
        TYPES.register(modEventBus);
    }

}
