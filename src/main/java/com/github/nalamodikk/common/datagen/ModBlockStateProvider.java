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

        // 🔥 只有當水晶材質不為 null 時才添加
        if (crystalTexture != null) {
            coreBuilder.texture("crystal", modLoc("block/" + crystalTexture));
        }


        // 🎨 修復材質UV設定 - 讓材質完全填滿每個面

        // 修復 createConduitModel 中的核心元素：
        var coreElement = coreBuilder.element()
                .from(coreSize[0], coreSize[0], coreSize[0])
                .to(coreSize[1], coreSize[1], coreSize[1]);

        // 🔧 為所有面添加正確的UV設定，讓材質填滿整個面
        coreElement.face(Direction.NORTH).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.SOUTH).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.WEST).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.EAST).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.UP).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.DOWN).texture("#core").uvs(0, 0, 16, 16).end()
                .end();


        // 🔥 只有當水晶材質和大小都不為 null 時才添加水晶
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

    private float[] getSmartUV(String modelName, String face) {

        // 🎨 關鍵：不同方向使用不同的材質區域，形成連貫的視覺效果

        if (modelName.contains("_west") || modelName.contains("_east")) {
            // 東西向管道 - 水平延伸效果
            if (face.equals("north") || face.equals("south")) {
                // 前後面：只顯示水平線，讓視覺上看起來是延伸的
                return new float[]{0, 6, 16, 10};  // 只取中間水平帶
            }
            if (face.equals("up") || face.equals("down")) {
                // 上下面：顯示水平線
                return new float[]{0, 6, 16, 10};
            }
            // 左右面：顯示完整十字，這是連接面
            return new float[]{0, 0, 16, 16};

        } else if (modelName.contains("_north") || modelName.contains("_south")) {
            // 南北向管道 - 垂直延伸效果
            if (face.equals("west") || face.equals("east")) {
                // 左右面：只顯示垂直線
                return new float[]{6, 0, 10, 16};  // 只取中間垂直帶
            }
            if (face.equals("up") || face.equals("down")) {
                // 上下面：顯示垂直線
                return new float[]{6, 0, 10, 16};
            }
            // 前後面：顯示完整十字，這是連接面
            return new float[]{0, 0, 16, 16};

        } else if (modelName.contains("_up") || modelName.contains("_down")) {
            // 上下向管道 - 垂直延伸效果
            if (face.equals("north") || face.equals("south") ||
                    face.equals("west") || face.equals("east")) {
                // 側面：只顯示垂直線
                return new float[]{6, 0, 10, 16};
            }
            // 上下面：顯示完整十字，這是連接面
            return new float[]{0, 0, 16, 16};
        }

        // 核心或其他情況：完整十字
        return new float[]{0, 0, 16, 16};
    }

    // 📚 步驟2: 添加UV分量提取方法
    private float getSmartU1(String modelName, String face) {
        return getSmartUV(modelName, face)[0]; // 左上角 U 座標
    }

    private float getSmartV1(String modelName, String face) {
        return getSmartUV(modelName, face)[1]; // 左上角 V 座標
    }

    private float getSmartU2(String modelName, String face) {
        return getSmartUV(modelName, face)[2]; // 右下角 U 座標
    }

    private float getSmartV2(String modelName, String face) {
        return getSmartUV(modelName, face)[3]; // 右下角 V 座標
    }
    /**
     * 改進的管道模型創建方法
     */

// 3️⃣ 優化的管道模型創建
    private ModelFile createPipeModel(String modelName, String pipeTexture,
                                      int x1, int y1, int z1, int x2, int y2, int z2) {

        return models().getBuilder(modelName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/" + pipeTexture))
                .texture("pipe", modLoc("block/" + pipeTexture))
                .element()
                .from(x1, y1, z1).to(x2, y2, z2)
                // 🎨 關鍵：每個面都使用智能UV，實現無縫連接
                .face(Direction.NORTH).texture("#pipe").uvs(getSmartU1(modelName, "north"),
                        getSmartV1(modelName, "north"),
                        getSmartU2(modelName, "north"),
                        getSmartV2(modelName, "north")).end()
                .face(Direction.SOUTH).texture("#pipe").uvs(getSmartU1(modelName, "south"),
                        getSmartV1(modelName, "south"),
                        getSmartU2(modelName, "south"),
                        getSmartV2(modelName, "south")).end()
                .face(Direction.WEST).texture("#pipe").uvs(getSmartU1(modelName, "west"),
                        getSmartV1(modelName, "west"),
                        getSmartU2(modelName, "west"),
                        getSmartV2(modelName, "west")).end()
                .face(Direction.EAST).texture("#pipe").uvs(getSmartU1(modelName, "east"),
                        getSmartV1(modelName, "east"),
                        getSmartU2(modelName, "east"),
                        getSmartV2(modelName, "east")).end()
                .face(Direction.UP).texture("#pipe").uvs(getSmartU1(modelName, "up"),
                        getSmartV1(modelName, "up"),
                        getSmartU2(modelName, "up"),
                        getSmartV2(modelName, "up")).end()
                .face(Direction.DOWN).texture("#pipe").uvs(getSmartU1(modelName, "down"),
                        getSmartV1(modelName, "down"),
                        getSmartU2(modelName, "down"),
                        getSmartV2(modelName, "down")).end()
                .end();
    }
// 🔥 使用方法：

    private void createArcaneConduitModel() {
        createConduitModel(
                "arcane_conduit",                    // 導管名稱
                ModBlocks.ARCANE_CONDUIT.get(),      // 方塊實例
                "conduit/arcane_conduit_core",       // 核心材質
                "conduit/arcane_conduit_pipe",       // 管道材質
                null,                                // 🔥 不要水晶材質
                new int[]{6, 10},                    // 核心大小 6-10
                null,                                // 🔥 不要水晶大小
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
