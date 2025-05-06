package com.github.nalamodikk.common.recipe;

import com.github.nalamodikk.common.register.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class ManaCraftingTableRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final NonNullList<Ingredient> inputItems;  // 支持多个输入物品
    private final ItemStack output;                    // 输出物品
    private final int manaCost;                        // 魔力消耗字段
    private final boolean isShaped;

    public ManaCraftingTableRecipe(ResourceLocation id, NonNullList<Ingredient> inputItems, ItemStack output, int manaCost,boolean isShaped) {
        this.id = id;
        this.inputItems = inputItems;
        this.output = output;
        this.manaCost = manaCost; // 初始化魔力消耗字段
        this.isShaped = isShaped;

    }

    @Override
    public boolean matches(SimpleContainer inv, Level world) {
        if (world.isClientSide()) {
            return false;
        }

        if (inv.getContainerSize() != 9) {
            return false;
        }

        if (isShaped) {
            // 有序合成
            for (int i = 0; i < inputItems.size(); i++) {
                Ingredient expected = inputItems.get(i);
                ItemStack actual = inv.getItem(i);
                if (!expected.test(actual)) {
                    return false;
                }
            }
            return true;
        } else {
            // 無序合成
            List<Ingredient> remainingIngredients = new ArrayList<>(inputItems);
            int usedSlotCount = 0;

            for (int i = 0; i < 9; i++) {
                ItemStack stackInSlot = inv.getItem(i);
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

            return remainingIngredients.isEmpty() && usedSlotCount == inputItems.size();
        }
    }


    public NonNullList<Ingredient> getInputItems() {
        return inputItems;
    }

    @Override
    public ItemStack assemble(SimpleContainer pContainer, RegistryAccess pRegistryAccess) {
        return output.copy();  // 返回合成结果
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        if (isShaped) {
            return width == 3 && height == 3;
        } else {
            return width * height >= inputItems.size();
        }
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MANA_CRAFTING_SERIALIZER.get();  // 返回注册的序列化器
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MANA_CRAFTING_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputItems;
    }

    // 新增获取魔力消耗的方法
    public int getManaCost() {
        return manaCost;
    }

    public boolean isShaped() {
        return this.isShaped;
    }


    // 自定义 Type 类
    public static class Type implements RecipeType<ManaCraftingTableRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mana_crafting";  // 统一名称
    }

    // 序列化器类
    public static class Serializer implements RecipeSerializer<ManaCraftingTableRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        // 从 JSON 数据读取配方
        @Override
        public ManaCraftingTableRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            NonNullList<Ingredient> inputItems;
            boolean isShaped = json.has("shaped") && json.get("shaped").getAsBoolean();

            if (json.has("pattern")) {
                JsonArray pattern = json.getAsJsonArray("pattern");
                JsonObject key = json.getAsJsonObject("key");

                String[] rows = new String[pattern.size()];
                for (int i = 0; i < rows.length; i++) {
                    rows[i] = pattern.get(i).getAsString();
                }

                Map<Character, Ingredient> symbolMap = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : key.entrySet()) {
                    symbolMap.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
                }

                inputItems = NonNullList.withSize(9, Ingredient.EMPTY);
                for (int row = 0; row < rows.length; row++) {
                    String line = rows[row];
                    for (int col = 0; col < line.length(); col++) {
                        int idx = row * 3 + col;
                        if (idx < 9) {
                            inputItems.set(idx, symbolMap.getOrDefault(line.charAt(col), Ingredient.EMPTY));
                        }
                    }
                }
            } else {
                JsonArray ingredients = json.getAsJsonArray("ingredients");
                inputItems = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);
                for (int i = 0; i < inputItems.size(); i++) {
                    inputItems.set(i, Ingredient.fromJson(ingredients.get(i)));
                }
            }


            JsonObject outputObj = json.getAsJsonObject("output");
            String itemId = outputObj.get("item").getAsString();
            int count = outputObj.has("count") ? outputObj.get("count").getAsInt() : 1;

            ItemStack output = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId)), count);

            int manaCost = json.get("manaCost").getAsInt();  // 读取魔力消耗值

            return new ManaCraftingTableRecipe(recipeId, inputItems, output, manaCost, isShaped);
        }

        // 从网络缓冲区读取配方
        @Override
        public ManaCraftingTableRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            NonNullList<Ingredient> inputItems = NonNullList.withSize(size, Ingredient.EMPTY);

            for (int i = 0; i < inputItems.size(); i++) {
                inputItems.set(i, Ingredient.fromNetwork(buffer));
            }

            ItemStack output = buffer.readItem();
            int manaCost = buffer.readInt();
            boolean isShaped = buffer.readBoolean(); // <- ✨ 新增這行，讀取 isShaped！

            return new ManaCraftingTableRecipe(recipeId, inputItems, output, manaCost, isShaped);
        }

        // 将配方数据写入网络缓冲区
        @Override
        public void toNetwork(FriendlyByteBuf buffer, ManaCraftingTableRecipe recipe) {
            buffer.writeInt(recipe.getIngredients().size());

            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItemStack(recipe.getResultItem(null), false);
            buffer.writeInt(recipe.getManaCost());
            buffer.writeBoolean(recipe.isShaped()); // ✨ 新增這行，把isShaped寫進去
        }

    }
}