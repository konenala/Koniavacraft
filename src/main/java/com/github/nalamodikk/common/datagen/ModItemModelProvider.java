package com.github.nalamodikk.common.datagen;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, KoniavacraftMod.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        ModItems.ITEMS.getEntries().forEach(item -> {
            Item instance = item.get();
            String name = item.getId().getPath();

            // ❌ 跳過 BlockItem（例如 mana_block）
            if (instance instanceof BlockItem) {
                return;
            }

            // ❌ 若對應貼圖不存在，也跳過（避免崩潰）
            ResourceLocation texture = modLoc("item/" + name);
            if (!existingFileHelper.exists(texture, TEXTURE)) {
                LOGGER.warn("Skipping item model for '{}': missing texture", name);
                return;
            }

            // ✅ 自動判斷工具或普通物品
            if (instance instanceof TieredItem || instance instanceof SwordItem) {
                handheldItem(instance);
            } else {
                basicItem(instance);
            }
        });
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
}
