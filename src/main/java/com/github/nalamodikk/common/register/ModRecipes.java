package com.github.nalamodikk.common.register;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.recipe.ManaCraftingTableRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MagicalIndustryMod.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MagicalIndustryMod.MOD_ID);

    // 魔法合成台配方--註冊實例
    private static final RecipeType<ManaCraftingTableRecipe> MANA_CRAFTING_TYPE_INSTANCE =
            new RecipeType<>() {
                @Override
                public String toString() {
                    return MagicalIndustryMod.MOD_ID + ":mana_crafting";
                }
            };
    // 註冊 serializer
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ManaCraftingTableRecipe>> MANA_CRAFTING_SERIALIZER =
            SERIALIZERS.register("mana_crafting", ManaCraftingTableRecipe.Serializer::new);
    // 註冊 recipe type（簡潔乾淨）
    public static final DeferredHolder<RecipeType<?>, RecipeType<ManaCraftingTableRecipe>> MANA_CRAFTING_TYPE =
            TYPES.register("mana_crafting", () -> MANA_CRAFTING_TYPE_INSTANCE);

    // 綁定用註冊方法
    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
        TYPES.register(modEventBus);
    }

}
