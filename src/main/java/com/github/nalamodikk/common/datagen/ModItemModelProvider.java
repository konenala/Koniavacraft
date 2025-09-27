package com.github.nalamodikk.common.datagen;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import net.neoforged.neoforge.registries.DeferredItem;



public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, KoniavacraftMod.MOD_ID, helper);
    }
    private final Set<ResourceLocation> customModelOverrides = new HashSet<>();


    @Override
    protected void registerModels() {
        // 1) 這裡列出「已手做模型」的物品（需要你自行盤點）
//        useExistingItemModel(ModItems.RESONANT_CRYSTAL);

        // 2) 其他物品：自動生成（有現成 model 就跳過）
        ModItems.ITEMS.getEntries().forEach(item -> {
            if (customModelOverrides.contains(item.getId())) return;

            Item instance = item.get();
            String name = item.getId().getPath();

            // 跳過 BlockItem（交給 Block 模型流程或另行處理）
            if (instance instanceof BlockItem) return;

            // 如果 models/item/<name>.json 已存在 → 直接跳過（雙重保險）
            if (existingFileHelper.exists(
                modLoc("item/" + name),
                new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".json", "models")
            )) {
                return;
            }

            // 沒有現成模型時，才用貼圖生成
            ResourceLocation texture = modLoc("item/" + name);
            if (!existingFileHelper.exists(texture, TEXTURE)) {
                LOGGER.warn("Skipping item model for '{}': missing texture", name);
                return;
            }

            // 工具類用 handheld，其餘用 basic
            if (instance instanceof TieredItem || instance instanceof SwordItem) {
                handheldItem((DeferredItem<?>) item);        // 注意：這裡要傳 DeferredItem<?>
            } else {
                basicItem(instance);       // 注意：這裡要傳 Item
            }
        });
    }


    private void registerGeneratedItemModel(DeferredItem<?> item, Consumer<ItemModelBuilder> customizer) {
        ResourceLocation id = item.getId();
        ResourceLocation texture = modLoc("item/" + id.getPath());
        if (!existingFileHelper.exists(texture, TEXTURE)) {
            LOGGER.warn("Skipping item model for '{}': missing texture", id);
            return;
        }

        ItemModelBuilder builder = withExistingParent(id.getPath(), mcLoc("item/generated"))
                .texture("layer0", texture);
        customizer.accept(builder);
        customModelOverrides.add(id);
    }

    private void applyResonantCrystalTransforms(ItemModelBuilder builder) {
        builder.transforms()
                .transform(ItemDisplayContext.GUI)
                    .rotation(30.0F, 45.0F, 0.0F)
                    .scale(0.85F)
                    .end()
                .transform(ItemDisplayContext.GROUND)
                    .translation(0.0F, 2.0F, 0.0F)
                    .scale(0.6F)
                    .end()
                .transform(ItemDisplayContext.FIXED)
                    .scale(0.8F)
                    .end()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                    .rotation(75.0F, 45.0F, 0.0F)
                    .translation(0.0F, 2.5F, 0.5F)
                    .scale(0.7F)
                    .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    .rotation(0.0F, 45.0F, 0.0F)
                    .translation(0.0F, 2.0F, 0.0F)
                    .scale(0.7F)
                    .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                    .rotation(0.0F, 225.0F, 0.0F)
                    .translation(0.0F, 2.0F, 0.0F)
                    .scale(0.7F)
                    .end();
    }

    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,"block/" + item.getId().getPath()));
    }

    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,"item/" + item.getId().getPath()));
    }

    // 在 ModItemModelProvider 內


    // 通用：這個物品已經有手做模型，請不要再生成
    private void useExistingItemModel(DeferredItem<?> item) {
        String name = item.getId().getPath();

        // 檢查 assets/<modid>/models/item/<name>.json 是否存在
        boolean exists = existingFileHelper.exists(
            modLoc("item/" + name),
            new ExistingFileHelper.ResourceType(PackType.CLIENT_RESOURCES, ".json", "models")
        );

        if (!exists) {
            LOGGER.warn("Expected existing model for item '{}', but not found at models/item/{}.json", name, name);
        }

        // 登記 → 之後自動流程會跳過它
        customModelOverrides.add(item.getId());
    }

}
