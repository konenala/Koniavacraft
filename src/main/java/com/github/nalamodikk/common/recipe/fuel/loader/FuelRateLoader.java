package com.github.nalamodikk.common.recipe.fuel.loader;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.recipe.fuel.FuelRecipe;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber
public class FuelRateLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FuelRateLoader.class);
    private static final Gson GSON = new Gson();
    private static final Map<String, FuelRate> FUEL_RATES = new HashMap<>();

    private static final String DEFAULT_NAMESPACE = MagicalIndustryMod.MOD_ID;
    private static final int DEFAULT_BURN_TIME = 200;  // 默認燃燒時間
    private static final int DEFAULT_ENERGY_RATE = 1;

    public FuelRateLoader() {
        super(GSON, "recipes/mana_recipes/fuel");  // 確保加載 mana_recipes/fuel 目錄
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FuelRateLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        FUEL_RATES.clear(); // 清除舊的數據

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(entry.getValue(), "fuel_rate");
                JsonObject ingredientObject = GsonHelper.getAsJsonObject(jsonObject, "ingredient");
                String itemId = GsonHelper.getAsString(ingredientObject, "item", "");
                String tagId = GsonHelper.getAsString(ingredientObject, "tag", "");
                int manaRate = GsonHelper.getAsInt(jsonObject, "mana", 0); // 默認魔力生產為 0
                int burnTime = GsonHelper.getAsInt(jsonObject, "burn_time", 200); // 默認燃燒時間 200
                int energyRate = GsonHelper.getAsInt(jsonObject, "energy", 1); // 默認能量生產為 1

                FuelRate fuelRate = new FuelRate(manaRate, burnTime, energyRate);

                if (!itemId.isEmpty()) {
                    FUEL_RATES.put(itemId, fuelRate);
                } else if (!tagId.isEmpty()) {
                    FUEL_RATES.put("tag:" + tagId, fuelRate);
                } else {
                    throw new JsonParseException("Fuel rate must have either an item or a tag");
                }

            } catch (IllegalArgumentException | JsonParseException e) {
                LOGGER.error("Couldn't parse fuel rate for {}", id, e);
            }
        }
        LOGGER.info("Loaded {} fuel rates", FUEL_RATES.size());
    }

    // Method to get fuel rate for an item
    public static FuelRate getFuelRateForItem(ResourceLocation itemId) {
        // 1️⃣ **先嘗試直接使用物品 ID 查找**
        FuelRate rate = FUEL_RATES.get(itemId.toString());
        if (rate != null) {
//            LOGGER.info("[FuelRateLoader] ✅ 透過物品 ID 找到燃料: {} | manaRate: {} | burnTime: {}",
//                    itemId, rate.getManaRate(), rate.getBurnTime());
            return rate;
        }

        // 2️⃣ **如果物品 ID 查找失敗，則嘗試標籤查找**
        for (Map.Entry<String, FuelRate> entry : FUEL_RATES.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("tag:")) {
                String tagName = key.substring(4);
                TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), new ResourceLocation(DEFAULT_NAMESPACE, tagName));
                if (BuiltInRegistries.ITEM.getTag(tag).stream()
                        .flatMap(holderSet -> holderSet.stream())
                        .anyMatch(holder -> holder.value().builtInRegistryHolder().key().location().equals(itemId))) {
//                    LOGGER.info("[FuelRateLoader] ✅ 透過標籤找到燃料: {} (標籤: {}) | manaRate: {} | burnTime: {}",
//                            itemId, tagName, entry.getValue().getManaRate(), entry.getValue().getBurnTime());
                    return entry.getValue();
                }
            }
        }

        // 3️⃣ **如果 `FuelRecipe` 內沒有對應物品，則使用 `ForgeHooks.getBurnTime()` 查找**
        int defaultBurnTime = ForgeHooks.getBurnTime(new ItemStack(BuiltInRegistries.ITEM.get(itemId)), RecipeType.SMELTING);
        if (defaultBurnTime > 0) {
            LOGGER.info("[FuelRateLoader] 🔥 使用 ForgeHooks 獲取燃燒時間: {} | burnTime: {}", itemId, defaultBurnTime);
            return new FuelRate(0, defaultBurnTime, DEFAULT_ENERGY_RATE);
        }

        // 4️⃣ **如果完全找不到數據，使用預設燃燒時間**
        LOGGER.warn("[FuelRateLoader] ❌ 找不到燃料數據: {}，使用預設值 manaRate: 0 | burnTime: {}", itemId, DEFAULT_BURN_TIME);
        return new FuelRate(0, DEFAULT_BURN_TIME, DEFAULT_ENERGY_RATE);
    }

    // Class representing fuel rate
    public static class FuelRate {
        public final int manaRate;
        public final int burnTime;
        public final int energyRate;

        public FuelRate(int manaRate, int burnTime, int energyRate) {
            this.manaRate = manaRate;
            this.burnTime = burnTime;
            this.energyRate = energyRate;
        }

        public int getManaRate() {
            return manaRate;
        }

        public int getBurnTime() {
            return burnTime;
        }

        public int getEnergyRate() {
            return energyRate;
        }
    }
}
