package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.client.model.generators.*;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, KoniavacraftMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // 🏗️ 基礎方塊 (六面相同材質)
        blockWithItem(ModBlocks.MANA_BLOCK);
        blockWithItem(ModBlocks.MAGIC_ORE);
        blockWithItem(ModBlocks.DEEPSLATE_MAGIC_ORE);
        blockWithItem(ModBlocks.MANA_SOIL);
        blockWithItem(ModBlocks.DEEP_MANA_SOIL);

        // 🌱 草方塊 (使用通用生成器)
        createGrassBlock(ModBlocks.MANA_GRASS_BLOCK, "mana_grass_block", "mana_soil");

        // 🔗 導管系統
        createArcaneConduitModel();

        // 🧪 特殊方塊 (自定義模型)
        createManaModel(ModBlocks.MANA_CRAFTING_TABLE_BLOCK);
        createManaModelWithFacing(ModBlocks.MANA_INFUSER);

        // === 魔法儀式系統 ===
        // 儀式核心 (Ritual Core) - 簡單方塊模型
        createManaModelWithFacing(ModBlocks.ARCANE_PEDESTAL);
        createManaModelWithFacingAndWorking(ModBlocks.RITUAL_CORE);


        // 奧術基座 (Arcane Pedestal) - 自定義模型
        // 注意：這裡假設您會提供 'arcane_pedestal.json' 模型文件，
        // 並且該文件定義了基座的複雜形狀和紋理。
        // simpleBlockWithItem 會自動生成方塊狀態和物品模型。

    }

    // ===========================================
    // 🌱 通用草方塊生成器
    // ===========================================

    /**
     * 通用的草方塊生成器
     *
     * @param grassBlock 草方塊的 DeferredBlock
     * @param grassBaseName 草方塊的基礎名稱（如 "mana_grass_block"）
     * @param soilTextureName 對應土壤的材質名稱（如 "mana_soil"）
     */
    private void createGrassBlock(DeferredBlock<Block> grassBlock, String grassBaseName, String soilTextureName) {
        // 自動構建材質路徑
        String topTexture = grassBaseName + "_top";     // mana_grass_block_top
        String sideTexture = grassBaseName + "_side";   // mana_grass_block_side
        String bottomTexture = soilTextureName;         // mana_soil

        // 創建草方塊模型
        ModelFile grassModel = models().cubeBottomTop(grassBaseName,
                modLoc("block/" + sideTexture),     // 側面材質
                modLoc("block/" + bottomTexture),   // 底部材質
                modLoc("block/" + topTexture)       // 頂部材質
        );

        // 生成方塊狀態和物品模型
        simpleBlock(grassBlock.get(), grassModel);
        simpleBlockItem(grassBlock.get(), grassModel);
    }

    // ===========================================
    // 🧪 特殊方塊模型
    // ===========================================

    /**
     * 🔧 創建基礎魔力方塊模型（無朝向屬性）
     */
    private void createManaModel(DeferredBlock<?> blockHolder) {
        Block block = blockHolder.get();
        String blockName = blockHolder.getId().getPath();
        getVariantBuilder(block)
                .partialState().modelForState()
                .modelFile(new ModelFile.UncheckedModelFile(modLoc("block/" + blockName)))
                .addModel();
    }

    /**
     * 🧭 創建有朝向屬性的魔力方塊模型
     */
    private void createManaModelWithFacing(DeferredBlock<?> blockHolder) {
        Block block = blockHolder.get();
        String blockName = blockHolder.getId().getPath();

        getVariantBuilder(block)
                // 北面 (默認方向)
                .partialState().with(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .modelForState().modelFile(new ModelFile.UncheckedModelFile(modLoc("block/" + blockName))).addModel()

                // 南面 (旋轉180度)
                .partialState().with(HorizontalDirectionalBlock.FACING, Direction.SOUTH)
                .modelForState().modelFile(new ModelFile.UncheckedModelFile(modLoc("block/" + blockName)))
                .rotationY(180).addModel()

                // 西面 (旋轉270度)
                .partialState().with(HorizontalDirectionalBlock.FACING, Direction.WEST)
                .modelForState().modelFile(new ModelFile.UncheckedModelFile(modLoc("block/" + blockName)))
                .rotationY(270).addModel()

                // 東面 (旋轉90度)
                .partialState().with(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .modelForState().modelFile(new ModelFile.UncheckedModelFile(modLoc("block/" + blockName)))
                .rotationY(90).addModel();
    }

    /**
     * 🔮 創建有朝向和工作狀態的魔力方塊模型 (適用於魔力注入機)
     */
    private void createManaModelWithFacingAndWorking(DeferredBlock<?> blockHolder) {
        Block block = blockHolder.get();
        String blockName = blockHolder.getId().getPath();

        // 獲取方塊的屬性
        BooleanProperty workingProperty = BooleanProperty.create("working");
        DirectionProperty facingProperty = BlockStateProperties.HORIZONTAL_FACING;

        VariantBlockStateBuilder builder = getVariantBuilder(block);

        // 為每個朝向和工作狀態組合創建變體
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (boolean working : new boolean[]{false, true}) {

                // 決定使用哪個模型文件
                String modelName = working ? blockName + "_working" : blockName;

                // 計算旋轉角度
                int rotationY = switch (direction) {
                    case NORTH -> 0;
                    case SOUTH -> 180;
                    case WEST -> 270;
                    case EAST -> 90;
                    default -> 0;
                };

                // 添加變體
                ConfiguredModel.Builder<?> modelBuilder = builder
                        .partialState()
                        .with(facingProperty, direction)
                        .with(workingProperty, working)
                        .modelForState()
                        .modelFile(new ModelFile.UncheckedModelFile(modLoc("block/" + modelName)));

                if (rotationY != 0) {
                    modelBuilder.rotationY(rotationY);
                }

                modelBuilder.addModel();
            }
        }
    }

    // ===========================================
    // 🔗 導管系統
    // ===========================================

    private void createArcaneConduitModel() {
        createConduitModel(
                "arcane_conduit",                    // 導管名稱
                ModBlocks.ARCANE_CONDUIT.get(),      // 方塊實例
                "conduit/arcane_conduit_core",       // 核心材質
                "conduit/arcane_conduit_pipe",       // 管道材質
                null,                                // 不要水晶材質
                new int[]{6, 10},                    // 核心大小 6-10
                null,                                // 不要水晶大小
                new int[]{6, 10}                     // 管道粗細 6-10
        );
    }

    /**
     * 通用的導管模型生成器
     */
    private void createConduitModel(String conduitName, Block conduitBlock,
                                    String coreTexture, String pipeTexture, String crystalTexture,
                                    int[] coreSize, int[] crystalSize, int[] pipeSize) {

        // 創建核心模型
        var coreBuilder = models().getBuilder(conduitName + "_core")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/" + coreTexture))
                .texture("core", modLoc("block/" + coreTexture));

        // 只有當水晶材質不為 null 時才添加
        if (crystalTexture != null) {
            coreBuilder.texture("crystal", modLoc("block/" + crystalTexture));
        }

        // 核心元素
        var coreElement = coreBuilder.element()
                .from(coreSize[0], coreSize[0], coreSize[0])
                .to(coreSize[1], coreSize[1], coreSize[1])
                .shade(false);

        // 為所有面添加材質
        coreElement.face(Direction.NORTH).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.SOUTH).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.WEST).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.EAST).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.UP).texture("#core").uvs(0, 0, 16, 16).end()
                .face(Direction.DOWN).texture("#core").uvs(0, 0, 16, 16).end()
                .end();

        // 只有當水晶材質和大小都不為 null 時才添加水晶
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

        // 根據連接狀態添加管道
        addConduitConnections(builder, northModel, southModel, westModel, eastModel, upModel, downModel);

        // 物品模型
        itemModels().getBuilder(conduitName).parent(coreModel);
    }

    /**
     * 添加導管的連接邏輯
     */
    private void addConduitConnections(MultiPartBlockStateBuilder builder,
                                       ModelFile north, ModelFile south, ModelFile west,
                                       ModelFile east, ModelFile up, ModelFile down) {

        builder.part().modelFile(north).addModel()
                .condition(com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock.NORTH, true);
        builder.part().modelFile(south).addModel()
                .condition(com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock.SOUTH, true);
        builder.part().modelFile(west).addModel()
                .condition(com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock.WEST, true);
        builder.part().modelFile(east).addModel()
                .condition(com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock.EAST, true);
        builder.part().modelFile(up).addModel()
                .condition(com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock.UP, true);
        builder.part().modelFile(down).addModel()
                .condition(com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock.DOWN, true);
    }

    /**
     * 創建管道模型
     */
    private ModelFile createPipeModel(String modelName, String pipeTexture,
                                      int x1, int y1, int z1, int x2, int y2, int z2) {

        // 判斷連接方向
        boolean isEastWest = modelName.contains("_east") || modelName.contains("_west");
        boolean isUpDown = modelName.contains("_up") || modelName.contains("_down");

        return models().getBuilder(modelName)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/" + pipeTexture))
                .texture("pipe", modLoc("block/" + pipeTexture))
                .element()
                .from(x1, y1, z1).to(x2, y2, z2)
                .shade(false)

                // North/South 面
                .face(Direction.NORTH).texture("#pipe").uvs(0, 0, 16, 16)
                .rotation(isEastWest ?
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.ZERO :
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90)
                .end()

                .face(Direction.SOUTH).texture("#pipe").uvs(0, 0, 16, 16)
                .rotation(isEastWest ?
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.ZERO :
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90)
                .end()

                // East/West 面
                .face(Direction.EAST).texture("#pipe").uvs(0, 0, 16, 16)
                .rotation(isUpDown ?
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90 :
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.ZERO)
                .end()

                .face(Direction.WEST).texture("#pipe").uvs(0, 0, 16, 16)
                .rotation(isUpDown ?
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90 :
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.ZERO)
                .end()

                // Up/Down 面
                .face(Direction.UP).texture("#pipe").uvs(0, 0, 16, 16)
                .rotation(isEastWest ?
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.ZERO :
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90)
                .end()

                .face(Direction.DOWN).texture("#pipe").uvs(0, 0, 16, 16)
                .rotation(isEastWest ?
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.ZERO :
                        net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90)
                .end()

                .end();
    }

    // ===========================================
    // 🛠️ 工具方法
    // ===========================================

    /**
     * 簡單方塊 (六面相同材質)
     */
    private void blockWithItem(DeferredBlock<?> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    /**
     * 樹苗方塊 (十字交叉模型)
     */
    private void saplingBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlock(blockRegistryObject.get(),
                models().cross(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(),
                        blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    /**
     * 樹葉方塊 (帶透明度)
     */
    private void leavesBlock(DeferredBlock<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(),
                models().singleTexture(BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath(),
                        ResourceLocation.parse("minecraft:block/leaves"),
                        "all", blockTexture(blockRegistryObject.get())).renderType("cutout"));
    }

    /**
     * 只有方塊物品模型
     */
    private void blockItem(DeferredBlock<?> deferredBlock) {
        simpleBlockItem(deferredBlock.get(),
                new ModelFile.UncheckedModelFile(KoniavacraftMod.MOD_ID + ":block/" + deferredBlock.getId().getPath()));
    }

    /**
     * 帶後綴的方塊物品模型
     */
    private void blockItem(DeferredBlock<?> deferredBlock, String appendix) {
        simpleBlockItem(deferredBlock.get(),
                new ModelFile.UncheckedModelFile(KoniavacraftMod.MOD_ID + ":block/" + deferredBlock.getId().getPath() + appendix));
    }
}
