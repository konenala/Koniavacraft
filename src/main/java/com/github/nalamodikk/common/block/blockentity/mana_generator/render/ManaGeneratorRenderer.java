package com.github.nalamodikk.common.block.blockentity.mana_generator.render;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlock;
import com.github.nalamodikk.common.block.blockentity.mana_generator.ManaGeneratorBlockEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.util.*;

/**
 * 🔮 魔力發電機修復版渲染器 - 正確的方塊旋轉和 UV 映射
 */
public class ManaGeneratorRenderer implements BlockEntityRenderer<ManaGeneratorBlockEntity> {

    // 🎨 材質資源
    private static final ResourceLocation TEXTURE_IDLE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_generator_texture.png");

    private static final ResourceLocation TEXTURE_ACTIVE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/mana_generator_active.png");

    // 📖 模型文件路徑
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/generator/mana_generator.json");

    // 📦 解析後的模型數據
    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private boolean modelLoaded = false;

    public ManaGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        // 🔄 載入並解析 JSON 模型
        loadAndParseModel();
    }

    /**
     * 📖 載入並解析你的 Blockbench JSON 模型
     */
    private void loadAndParseModel() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(MODEL_LOCATION);
            if (resource.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                    JsonObject modelData = JsonParser.parseReader(reader).getAsJsonObject();
                    parseModelData(modelData);
                    modelLoaded = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 🔧 解析 JSON 模型數據，按群組分類元素
     */
    private void parseModelData(JsonObject modelData) {
        JsonArray elements = modelData.getAsJsonArray("elements");
        JsonArray groups = modelData.getAsJsonArray("groups");

        // 🎯 建立群組到元素的映射
        Map<Integer, String> elementToGroup = new HashMap<>();

        // 解析群組，建立元素索引到群組名稱的映射
        if (groups != null) {
            for (int i = 0; i < groups.size(); i++) {
                JsonObject group = groups.get(i).getAsJsonObject();
                String groupName = group.get("name").getAsString();
                JsonArray children = group.getAsJsonArray("children");

                for (int j = 0; j < children.size(); j++) {
                    int elementIndex = children.get(j).getAsInt();
                    elementToGroup.put(elementIndex, groupName);
                }
            }
        }

        // 🧱 解析元素並按群組分類
        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();
            String groupName = elementToGroup.getOrDefault(i,
                    element.has("name") ? element.get("name").getAsString() : "unknown");

            ModelElement modelElement = parseElement(element);
            groupElements.computeIfAbsent(groupName, k -> new ArrayList<>()).add(modelElement);
        }
    }

    /**
     * 🧱 解析單個元素，包含正確的 UV 映射
     */
    private ModelElement parseElement(JsonObject element) {
        // 解析位置和尺寸
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");

        float x1 = from.get(0).getAsFloat() / 16.0F;
        float y1 = from.get(1).getAsFloat() / 16.0F;
        float z1 = from.get(2).getAsFloat() / 16.0F;
        float x2 = to.get(0).getAsFloat() / 16.0F;
        float y2 = to.get(1).getAsFloat() / 16.0F;
        float z2 = to.get(2).getAsFloat() / 16.0F;

        // 解析旋轉（如果有）
        float rotationAngle = 0;
        String rotationAxis = "y";
        float[] rotationOrigin = {0, 0, 0};

        if (element.has("rotation")) {
            JsonObject rotation = element.getAsJsonObject("rotation");
            rotationAngle = rotation.get("angle").getAsFloat();
            rotationAxis = rotation.get("axis").getAsString();

            JsonArray origin = rotation.getAsJsonArray("origin");
            rotationOrigin[0] = origin.get(0).getAsFloat() / 16.0F;
            rotationOrigin[1] = origin.get(1).getAsFloat() / 16.0F;
            rotationOrigin[2] = origin.get(2).getAsFloat() / 16.0F;
        }

        // 🎨 解析面的 UV 映射
        Map<String, FaceUV> faceUVs = new HashMap<>();
        if (element.has("faces")) {
            JsonObject faces = element.getAsJsonObject("faces");
            for (String faceName : faces.keySet()) {
                JsonObject face = faces.getAsJsonObject(faceName);
                if (face.has("uv")) {
                    JsonArray uv = face.getAsJsonArray("uv");
                    float u1 = uv.get(0).getAsFloat() / 16.0F; // 標準化到 0-1
                    float v1 = uv.get(1).getAsFloat() / 16.0F;
                    float u2 = uv.get(2).getAsFloat() / 16.0F;
                    float v2 = uv.get(3).getAsFloat() / 16.0F;

                    int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
                    faceUVs.put(faceName, new FaceUV(u1, v1, u2, v2, rotation));
                }
            }
        }

        return new ModelElement(x1, y1, z1, x2, y2, z2, rotationAngle, rotationAxis, rotationOrigin, faceUVs);
    }

    @Override
    public void render(ManaGeneratorBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (!modelLoaded || blockEntity.getLevel() == null) return;

        // 🎯 計算動畫參數
        float time = (blockEntity.getLevel().getGameTime() + partialTick) * 0.05F;
        boolean isWorking = blockEntity.isWorking();

        poseStack.pushPose();

        // 🧭 **重要修復：根據方塊的 FACING 狀態旋轉整個模型**
        Direction facing = blockEntity.getBlockState().getValue(ManaGeneratorBlock.FACING);
        applyBlockRotation(poseStack, facing);

        // 🎨 根據工作狀態選擇材質
        ResourceLocation currentTexture = isWorking ? TEXTURE_ACTIVE : TEXTURE_IDLE;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid(currentTexture));

        // 🏗️ 渲染靜態群組
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bb_main", 0, 0, 0, 0);
        renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone2", 0, 0, 0, 0);

        // 🔮 渲染動畫晶體群組
        if (isWorking) {
            // 🚀 工作狀態：快速自轉動畫（不上下浮動）
            // 中央晶體（bone）- 快速自轉
            float centralRot = time * 10.0F; // 快速旋轉
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone", 0, 0, 0, centralRot);

            // 左側晶體（bone3）- 反向快速自轉
            float leftRot = -time * 10.8F; // 反向旋轉，稍慢一點
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone3", 0, 0, 0, leftRot);

            // 右側晶體（bone4）- 另一個速度的快速自轉
            float rightRot = time * 10.2F; // 稍快一點的旋轉
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone4", 0, 0, 0, rightRot);
        } else {
            // 💤 閒置狀態：緩慢上下浮動動畫
            // 中央晶體（bone）
            float centralFloat = (float) Math.sin(time) * 0.125F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone", 0, centralFloat, 0, 0);

            // 左側晶體（bone3）- 相位差120°
            float leftFloat = (float) Math.sin(time + Math.PI * 2.0F / 3.0F) * 0.125F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone3", 0, leftFloat, 0, 0);

            // 右側晶體（bone4）- 相位差240°
            float rightFloat = (float) Math.sin(time + Math.PI * 4.0F / 3.0F) * 0.125F;
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, "bone4", 0, rightFloat, 0, 0);
        }

        poseStack.popPose();
    }

    /**
     * 🧭 根據方塊的 FACING 狀態應用旋轉
     */
    private void applyBlockRotation(PoseStack poseStack, Direction facing) {
        // 移動到方塊中心進行旋轉
        poseStack.translate(0.5, 0.0, 0.5);

        // 根據面向方向旋轉
        switch (facing) {
            case NORTH:
                // 北面是預設方向，不需要旋轉
                break;
            case SOUTH:
                // 向南旋轉 180 度
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
                break;
            case WEST:
                // 向西旋轉 90 度
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                break;
            case EAST:
                // 向東旋轉 -90 度（或 270 度）
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
                break;
        }

        // 移回原位
        poseStack.translate(-0.5, 0.0, -0.5);
    }

    /**
     * 🎨 渲染特定群組
     */
    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, String groupName,
                             float offsetX, float offsetY, float offsetZ, float rotationY) {

        List<ModelElement> elements = groupElements.get(groupName);
        if (elements == null || elements.isEmpty()) return;

        poseStack.pushPose();

        // 🎯 應用動畫變換
        poseStack.translate(offsetX, offsetY, offsetZ);

        // 🌍 使用 Blockbench 模型中定義的群組原點進行真正原地自轉
        if (rotationY != 0) {
            Vector3f groupOrigin = getBlockbenchGroupOrigin(groupName);

            // 移動到 Blockbench 定義的群組原點
            poseStack.translate(groupOrigin.x(), groupOrigin.y(), groupOrigin.z());
            // 繞垂直軸（Y軸）旋轉
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(rotationY));
            // 移回原位
            poseStack.translate(-groupOrigin.x(), -groupOrigin.y(), -groupOrigin.z());
        }

        // 🧱 渲染這個群組的所有元素（作為整體）
        for (ModelElement element : elements) {
            renderElement(poseStack, vertexConsumer, packedLight, packedOverlay, element);
        }

        poseStack.popPose();
    }

    /**
     * 🎯 使用 Blockbench 模型中定義的群組原點（這些是設計時的旋轉中心）
     */
    private Vector3f getBlockbenchGroupOrigin(String groupName) {
        return switch (groupName) {
            case "bone" -> new Vector3f(8.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F); // 中央晶體原點
            case "bone3" -> new Vector3f(3.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F); // 左側晶體原點
            case "bone4" -> new Vector3f(13.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F); // 右側晶體原點
            default -> new Vector3f(0.0F, 0.0F, 0.0F); // 預設原點
        };
    }




    /**
     * 🎯 獲取群組的旋轉中心點（基於 Blockbench 模型的原點）
     */
    private Vector3f getGroupRotationCenter(String groupName) {
        return switch (groupName) {
            case "bone" -> new Vector3f(8.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F); // 中央晶體
            case "bone3" -> new Vector3f(3.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F); // 左側晶體
            case "bone4" -> new Vector3f(13.0F / 16.0F, 29.24749F / 16.0F, 8.29246F / 16.0F); // 右側晶體
            default -> new Vector3f(0.0F, 0.0F, 0.0F); // 預設中心
        };
    }

    /**
     * 🧱 渲染單個元素（僅用於非動畫元素）
     */
    private void renderElement(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, ModelElement element) {

        poseStack.pushPose();

        // 🔄 處理元素的旋轉（如果有）
        if (element.rotationAngle != 0) {
            poseStack.translate(element.rotationOrigin[0], element.rotationOrigin[1], element.rotationOrigin[2]);

            switch (element.rotationAxis) {
                case "x":
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(element.rotationAngle));
                    break;
                case "y":
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(element.rotationAngle));
                    break;
                case "z":
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(element.rotationAngle));
                    break;
            }

            poseStack.translate(-element.rotationOrigin[0], -element.rotationOrigin[1], -element.rotationOrigin[2]);
        }

        // 🧊 渲染立方體
        renderCube(poseStack, vertexConsumer, packedLight, packedOverlay, element);

        poseStack.popPose();
    }

    /**
     * 🧊 渲染立方體，包含正確的 UV 映射
     */
    private void renderCube(PoseStack poseStack, VertexConsumer vertexConsumer,
                            int packedLight, int packedOverlay, ModelElement element) {

        Matrix4f matrix = poseStack.last().pose();

        float x1 = element.x1, y1 = element.y1, z1 = element.z1;
        float x2 = element.x2, y2 = element.y2, z2 = element.z2;

        // 🎨 渲染6個面，使用正確的 UV 坐標
        // 頂面 (Y+)
        FaceUV topUV = element.faceUVs.getOrDefault("up", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y2, z1,  x1, y2, z2,  x2, y2, z2,  x2, y2, z1,
                0, 1, 0, topUV);

        // 底面 (Y-)
        FaceUV bottomUV = element.faceUVs.getOrDefault("down", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1,  x2, y1, z1,  x2, y1, z2,  x1, y1, z2,
                0, -1, 0, bottomUV);

        // 北面 (Z-)
        FaceUV northUV = element.faceUVs.getOrDefault("north", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1,  x1, y2, z1,  x2, y2, z1,  x2, y1, z1,
                0, 0, -1, northUV);

        // 南面 (Z+)
        FaceUV southUV = element.faceUVs.getOrDefault("south", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z2,  x2, y1, z2,  x2, y2, z2,  x1, y2, z2,
                0, 0, 1, southUV);

        // 西面 (X-)
        FaceUV westUV = element.faceUVs.getOrDefault("west", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1,  x1, y1, z2,  x1, y2, z2,  x1, y2, z1,
                -1, 0, 0, westUV);

        // 東面 (X+)
        FaceUV eastUV = element.faceUVs.getOrDefault("east", new FaceUV(0, 0, 1, 1, 0));
        addQuadWithUV(vertexConsumer, matrix, packedLight, packedOverlay,
                x2, y1, z1,  x2, y2, z1,  x2, y2, z2,  x2, y1, z2,
                1, 0, 0, eastUV);
    }

    /**
     * 📐 添加四邊形，包含正確的 UV 映射
     */
    private void addQuadWithUV(VertexConsumer vertexConsumer, Matrix4f matrix, int packedLight, int packedOverlay,
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float x3, float y3, float z3, float x4, float y4, float z4,
                               float nx, float ny, float nz, FaceUV faceUV) {

        // 🎯 變換位置
        Vector3f pos1 = matrix.transformPosition(x1, y1, z1, new Vector3f());
        Vector3f pos2 = matrix.transformPosition(x2, y2, z2, new Vector3f());
        Vector3f pos3 = matrix.transformPosition(x3, y3, z3, new Vector3f());
        Vector3f pos4 = matrix.transformPosition(x4, y4, z4, new Vector3f());

        int color = -1; // 白色，不覆蓋材質

        // 🎨 根據旋轉應用正確的 UV 坐標
        float[][] uvCoords = getRotatedUVCoords(faceUV);

        // 使用正確的方法簽名：addVertex(x, y, z, color, u, v, packedOverlay, packedLight, normalX, normalY, normalZ)
        vertexConsumer.addVertex(pos1.x(), pos1.y(), pos1.z(), color, uvCoords[0][0], uvCoords[0][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos2.x(), pos2.y(), pos2.z(), color, uvCoords[1][0], uvCoords[1][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos3.x(), pos3.y(), pos3.z(), color, uvCoords[2][0], uvCoords[2][1], packedOverlay, packedLight, nx, ny, nz);
        vertexConsumer.addVertex(pos4.x(), pos4.y(), pos4.z(), color, uvCoords[3][0], uvCoords[3][1], packedOverlay, packedLight, nx, ny, nz);
    }

    /**
     * 🔄 根據旋轉獲取正確的 UV 坐標
     */
    private float[][] getRotatedUVCoords(FaceUV faceUV) {
        float u1 = faceUV.u1, v1 = faceUV.v1, u2 = faceUV.u2, v2 = faceUV.v2;

        // 四個角的 UV 坐標（未旋轉）
        float[][] baseCoords = {
                {u1, v2}, // 左下
                {u1, v1}, // 左上
                {u2, v1}, // 右上
                {u2, v2}  // 右下
        };

        // 根據旋轉角度調整 UV 坐標
        int rotation = faceUV.rotation;
        if (rotation == 0) {
            return baseCoords;
        }

        float[][] rotatedCoords = new float[4][2];
        for (int i = 0; i < 4; i++) {
            int sourceIndex = (i - rotation / 90 + 4) % 4;
            rotatedCoords[i] = baseCoords[sourceIndex];
        }

        return rotatedCoords;
    }

    /**
     * 📦 模型元素數據類，包含 UV 映射
     */
    private static class ModelElement {
        final float x1, y1, z1, x2, y2, z2;
        final float rotationAngle;
        final String rotationAxis;
        final float[] rotationOrigin;
        final Map<String, FaceUV> faceUVs;

        ModelElement(float x1, float y1, float z1, float x2, float y2, float z2,
                     float rotationAngle, String rotationAxis, float[] rotationOrigin,
                     Map<String, FaceUV> faceUVs) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
            this.rotationAngle = rotationAngle;
            this.rotationAxis = rotationAxis;
            this.rotationOrigin = rotationOrigin;
            this.faceUVs = faceUVs;
        }
    }

    /**
     * 🎨 面的 UV 映射數據類
     */
    private static class FaceUV {
        final float u1, v1, u2, v2;
        final int rotation;

        FaceUV(float u1, float v1, float u2, float v2, int rotation) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
            this.rotation = rotation;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ManaGeneratorBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}