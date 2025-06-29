package com.github.nalamodikk.common.datagen.block.conduit;
// 在你的 datagen 包中創建這個類

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ConduitModelProvider extends BlockStateProvider {

    public ConduitModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, KoniavacraftMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        createArcaneConduitModel();
    }

    private void createArcaneConduitModel() {
        // 創建基礎部件模型
        ModelFile coreModel = models().getBuilder("arcane_conduit_core")
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/arcane_conduit_core"))
                .texture("core", modLoc("block/arcane_conduit_core"))
                .texture("crystal", modLoc("block/arcane_crystal"))
                .element()
                .from(6, 6, 6).to(10, 10, 10)
                .face(Direction.NORTH).texture("#core").end()
                .face(Direction.SOUTH).texture("#core").end()
                .face(Direction.WEST).texture("#core").end()
                .face(Direction.EAST).texture("#core").end()
                .face(Direction.UP).texture("#core").end()
                .face(Direction.DOWN).texture("#core").end()
                .end()
                // 中心發光水晶
                .element()
                .from(7, 7, 7).to(9, 9, 9)
                .face(Direction.NORTH).texture("#crystal").end()
                .face(Direction.SOUTH).texture("#crystal").end()
                .face(Direction.WEST).texture("#crystal").end()
                .face(Direction.EAST).texture("#crystal").end()
                .face(Direction.UP).texture("#crystal").end()
                .face(Direction.DOWN).texture("#crystal").end()
                .end();

        // 創建各方向的連接管道模型
        ModelFile northModel = createPipeModel("north", 6, 6, 0, 10, 10, 6);
        ModelFile southModel = createPipeModel("south", 6, 6, 10, 10, 10, 16);
        ModelFile westModel = createPipeModel("west", 0, 6, 6, 6, 10, 10);
        ModelFile eastModel = createPipeModel("east", 10, 6, 6, 16, 10, 10);
        ModelFile upModel = createPipeModel("up", 6, 10, 6, 10, 16, 10);
        ModelFile downModel = createPipeModel("down", 6, 0, 6, 10, 6, 10);

        // 使用 multipart 系統構建方塊狀態
        MultiPartBlockStateBuilder builder = getMultipartBuilder(ModBlocks.ARCANE_CONDUIT.get());

        // 始終渲染核心
        builder.part()
                .modelFile(coreModel)
                .addModel();

        // 根據連接狀態添加管道
        builder.part()
                .modelFile(northModel)
                .addModel()
                .condition(ArcaneConduitBlock.NORTH, true);

        builder.part()
                .modelFile(southModel)
                .addModel()
                .condition(ArcaneConduitBlock.SOUTH, true);

        builder.part()
                .modelFile(westModel)
                .addModel()
                .condition(ArcaneConduitBlock.WEST, true);

        builder.part()
                .modelFile(eastModel)
                .addModel()
                .condition(ArcaneConduitBlock.EAST, true);

        builder.part()
                .modelFile(upModel)
                .addModel()
                .condition(ArcaneConduitBlock.UP, true);

        builder.part()
                .modelFile(downModel)
                .addModel()
                .condition(ArcaneConduitBlock.DOWN, true);

        // 物品模型（背包中顯示）
        itemModels().getBuilder("arcane_conduit")
                .parent(coreModel);
    }

    private ModelFile createPipeModel(String name, int x1, int y1, int z1, int x2, int y2, int z2) {
        return models().getBuilder("arcane_conduit_" + name)
                .parent(models().getExistingFile(mcLoc("block/block")))
                .texture("particle", modLoc("block/arcane_conduit_pipe"))
                .texture("pipe", modLoc("block/arcane_conduit_pipe"))
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

    public ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
    }
}