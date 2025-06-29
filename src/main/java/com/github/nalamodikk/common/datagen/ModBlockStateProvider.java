package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;


public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, KoniavacraftMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.MANA_BLOCK);
        blockWithItem(ModBlocks.DEEPSLATE_MAGIC_ORE);
        blockWithItem(ModBlocks.MAGIC_ORE);
        createArcaneConduitModel();



        getVariantBuilder(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get())
                .partialState().modelForState()
                .modelFile(new ModelFile.UncheckedModelFile(modLoc("block/mana_crafting_table")))
                .addModel();

    }

    private void saplingBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(),
                models().cross(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(), blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    private void leavesBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(),
                models().singleTexture(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(), ResourceLocation.parse("minecraft:block/leaves"),
                        "all", blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }



    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile(KoniavacraftMod.MOD_ID + ":block/" + deferredBlock.getId().getPath()));
    }

    private void blockItem(DeferredBlock<?> deferredBlock, String appendix) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile(KoniavacraftMod.MOD_ID + ":block/" + deferredBlock.getId().getPath() + appendix));
    }

    // 🔥 通用導管模型生成方法

    /**
     * 通用的導管模型生成器
     * @param conduitName 導管名稱 (如 "arcane_conduit", "mystic_conduit")
     * @param conduitBlock 導管方塊實例
     * @param coreTexture 核心材質路徑
     * @param pipeTexture 管道材質路徑
     * @param crystalTexture 水晶材質路徑 (可選，null 表示沒有水晶)
     * @param coreSize 核心大小 [from, to] (如 [6,10] 表示 6到10像素)
     * @param crystalSize 水晶大小 [from, to] (如 [7,9] 表示 7到9像素)
     * @param pipeSize 管道粗細 [from, to] (如 [6,10] 表示 6到10像素)
     */
    private void createConduitModel(String conduitName, Block conduitBlock,
                                    String coreTexture, String pipeTexture, String crystalTexture,
                                    int[] coreSize, int[] crystalSize, int[] pipeSize) {

        // 創建核心模型
        var coreBuilder = models().getBuilder(conduitName + "_core")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/" + coreTexture))
                .texture("core", modLoc("block/" + coreTexture));

        // 添加水晶材質（如果有）
        if (crystalTexture != null) {
            coreBuilder.texture("crystal", modLoc("block/" + crystalTexture));
        }

        // 創建核心元素
        var coreElement = coreBuilder.element()
                .from(coreSize[0], coreSize[0], coreSize[0])
                .to(coreSize[1], coreSize[1], coreSize[1]);

        // 🔧 修復：直接調用面的設置
        coreElement.face(Direction.NORTH).texture("#core").end()
                .face(Direction.SOUTH).texture("#core").end()
                .face(Direction.WEST).texture("#core").end()
                .face(Direction.EAST).texture("#core").end()
                .face(Direction.UP).texture("#core").end()
                .face(Direction.DOWN).texture("#core").end()
                .end();

        // 如果有水晶，添加水晶元素
        if (crystalTexture != null && crystalSize != null) {
            var crystalElement = coreBuilder.element()
                    .from(crystalSize[0], crystalSize[0], crystalSize[0])
                    .to(crystalSize[1], crystalSize[1], crystalSize[1]);

            crystalElement.face(Direction.NORTH).texture("#crystal").end()
                    .face(Direction.SOUTH).texture("#crystal").end()
                    .face(Direction.WEST).texture("#crystal").end()
                    .face(Direction.EAST).texture("#crystal").end()
                    .face(Direction.UP).texture("#crystal").end()
                    .face(Direction.DOWN).texture("#crystal").end()
                    .end();
        }

        ModelFile coreModel = coreBuilder;


        // 創建各方向的管道模型
        ModelFile northModel = createPipeModel(conduitName + "_north", pipeTexture,
                pipeSize[0], pipeSize[0], 0, pipeSize[1], pipeSize[1], pipeSize[0]);
        ModelFile southModel = createPipeModel(conduitName + "_south", pipeTexture,
                pipeSize[0], pipeSize[0], pipeSize[1], pipeSize[1], pipeSize[1], 16);
        ModelFile westModel = createPipeModel(conduitName + "_west", pipeTexture,
                0, pipeSize[0], pipeSize[0], pipeSize[0], pipeSize[1], pipeSize[1]);
        ModelFile eastModel = createPipeModel(conduitName + "_east", pipeTexture,
                pipeSize[1], pipeSize[0], pipeSize[0], 16, pipeSize[1], pipeSize[1]);
        ModelFile upModel = createPipeModel(conduitName + "_up", pipeTexture,
                pipeSize[0], pipeSize[1], pipeSize[0], pipeSize[1], 16, pipeSize[1]);
        ModelFile downModel = createPipeModel(conduitName + "_down", pipeTexture,
                pipeSize[0], 0, pipeSize[0], pipeSize[1], pipeSize[0], pipeSize[1]);

        // 構建多部分方塊狀態
        MultiPartBlockStateBuilder builder = getMultipartBuilder(conduitBlock);

        // 始終渲染核心
        builder.part().modelFile(coreModel).addModel();

        // 根據連接狀態添加管道 (通用的連接屬性)
        addConduitConnections(builder, northModel, southModel, westModel, eastModel, upModel, downModel);

        // 物品模型
        itemModels().getBuilder(conduitName).parent(coreModel);
    }

    /**
     * 為立方體元素添加6個面的材質
     */
    private void addCubeFaces(BlockModelBuilder.ElementBuilder element, String texture) {
        element.face(Direction.NORTH).texture(texture).end()
                .face(Direction.SOUTH).texture(texture).end()
                .face(Direction.WEST).texture(texture).end()
                .face(Direction.EAST).texture(texture).end()
                .face(Direction.UP).texture(texture).end()
                .face(Direction.DOWN).texture(texture).end();
    }


    /**
     * 添加導管的連接邏輯 (假設所有導管都使用相同的連接屬性名)
     */

    /**
     * 添加導管的連接邏輯
     */
    private void addConduitConnections(MultiPartBlockStateBuilder builder,
                                       ModelFile north, ModelFile south, ModelFile west,
                                       ModelFile east, ModelFile up, ModelFile down) {

        // ✅ 修復：引用 ArcaneConduitBlock 中定義的屬性
        builder.part().modelFile(north).addModel()
                .condition(com.github.nalamodikk.common.block.conduit.ArcaneConduitBlock.NORTH, true);
        builder.part().modelFile(south).addModel()
                .condition(com.github.nalamodikk.common.block.conduit.ArcaneConduitBlock.SOUTH, true);
        builder.part().modelFile(west).addModel()
                .condition(com.github.nalamodikk.common.block.conduit.ArcaneConduitBlock.WEST, true);
        builder.part().modelFile(east).addModel()
                .condition(com.github.nalamodikk.common.block.conduit.ArcaneConduitBlock.EAST, true);
        builder.part().modelFile(up).addModel()
                .condition(com.github.nalamodikk.common.block.conduit.ArcaneConduitBlock.UP, true);
        builder.part().modelFile(down).addModel()
                .condition(com.github.nalamodikk.common.block.conduit.ArcaneConduitBlock.DOWN, true);
    }
    /**
     * 改進的管道模型創建方法
     */
    private ModelFile createPipeModel(String modelName, String pipeTexture,
                                      int x1, int y1, int z1, int x2, int y2, int z2) {
        return models().getBuilder(modelName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/" + pipeTexture))
                .texture("pipe", modLoc("block/" + pipeTexture))
                .element()
                .from(x1, y1, z1).to(x2, y2, z2)
                .face(Direction.NORTH).texture("#pipe").end()
                .face(Direction.SOUTH).texture("#pipe").end()
                .face(Direction.WEST).texture("#pipe").end()
                .face(Direction.EAST).texture("#pipe").end()
                .face(Direction.UP).texture("#pipe").end()
                .face(Direction.DOWN).texture("#pipe").end()
                .end();
    }

// 🔥 使用方法：

    private void createArcaneConduitModel() {
        createConduitModel(
                "arcane_conduit",                    // 導管名稱
                ModBlocks.ARCANE_CONDUIT.get(),      // 方塊實例
                "conduit/arcane_conduit_core",               // 核心材質
                "conduit/arcane_conduit_pipe",               // 管道材質
                "conduit/arcane_crystal",                    // 水晶材質
                new int[]{6, 10},                    // 核心大小 6-10
                new int[]{7, 9},                     // 水晶大小 7-9
                new int[]{6, 10}                     // 管道粗細 6-10
        );
    }

// 🔥 未來添加其他導管時：
//
//    private void createMysticConduitModel() {
//        createConduitModel(
//                "mystic_conduit",                    // 不同的名稱
//                ModBlocks.MYSTIC_CONDUIT.get(),      // 不同的方塊
//                "mystic_conduit_core",               // 不同的核心材質
//                "mystic_conduit_pipe",               // 不同的管道材質
//                "mystic_crystal",                    // 不同的水晶材質
//                new int[]{5, 11},                    // 稍大的核心
//                new int[]{6, 10},                    // 稍大的水晶
//                new int[]{5, 11}                     // 稍粗的管道
//        );
//    }
//
//    private void createVoidConduitModel() {
//        createConduitModel(
//                "void_conduit",
//                ModBlocks.VOID_CONDUIT.get(),
//                "void_conduit_core",
//                "void_conduit_pipe",
//                null,                                // 沒有水晶
//                new int[]{4, 12},                    // 更大的核心
//                null,                                // 沒有水晶大小
//                new int[]{4, 12}                     // 更粗的管道
//        );
//    }
}
