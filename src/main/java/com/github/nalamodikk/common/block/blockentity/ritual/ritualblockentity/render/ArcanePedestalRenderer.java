package com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.render;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.ArcanePedestalBlockEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.io.InputStreamReader;
import java.util.*;

/**
 * 奧術基座渲染器 - 渲染方塊模型與浮動的祭品物品
 */
public class ArcanePedestalRenderer implements BlockEntityRenderer<ArcanePedestalBlockEntity> {

    private final ItemRenderer itemRenderer;

    // 🎨 材質資源
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/block/arcane_pedestal_texture.png");

    // 📖 模型文件路徑
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "models/block/arcane_pedestal.json");

    // 📦 解析後的模型數據
    private final Map<String, List<ModelElement>> groupElements = new HashMap<>();
    private boolean modelLoaded = false;

    public ArcanePedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        // 🔄 載入並解析 JSON 模型
        loadAndParseModel();
    }

    /**
     * 📖 載入並解析 Blockbench JSON 模型
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
            KoniavacraftMod.LOGGER.error("Failed to load Arcane Pedestal model", e);
        }
    }

    /**
     * 🔧 解析 JSON 模型數據
     */
    private void parseModelData(JsonObject modelData) {
        JsonArray elements = modelData.getAsJsonArray("elements");
        JsonArray groups = modelData.has("groups") ? modelData.getAsJsonArray("groups") : null;

        // 🎯 建立群組到元素的映射
        Map<Integer, String> elementToGroup = new HashMap<>();

        if (groups != null) {
            parseGroups(groups, elementToGroup);
        }

        // 🧱 解析元素並按群組分類
        for (int i = 0; i < elements.size(); i++) {
            JsonObject element = elements.get(i).getAsJsonObject();
            String groupName = elementToGroup.getOrDefault(i,
                    element.has("name") ? element.get("name").getAsString() : "main");

            ModelElement modelElement = parseElement(element);
            groupElements.computeIfAbsent(groupName, k -> new ArrayList<>()).add(modelElement);
        }
    }

    /**
     * 🔧 解析群組結構
     */
    private void parseGroups(JsonArray groups, Map<Integer, String> elementToGroup) {
        for (int i = 0; i < groups.size(); i++) {
            JsonObject group = groups.get(i).getAsJsonObject();
            String groupName = group.get("name").getAsString();

            if (group.has("children")) {
                JsonArray children = group.getAsJsonArray("children");
                parseGroupChildren(children, groupName, elementToGroup);
            }
        }
    }

    /**
     * 🔧 遞歸解析群組子元素
     */
    private void parseGroupChildren(JsonArray children, String groupName, Map<Integer, String> elementToGroup) {
        for (int j = 0; j < children.size(); j++) {
            var child = children.get(j);
            if (child.isJsonPrimitive()) {
                int elementIndex = child.getAsInt();
                elementToGroup.put(elementIndex, groupName);
            } else if (child.isJsonObject()) {
                // 嵌套群組
                JsonObject nestedGroup = child.getAsJsonObject();
                if (nestedGroup.has("children")) {
                    parseGroupChildren(nestedGroup.getAsJsonArray("children"), groupName, elementToGroup);
                }
            }
        }
    }

    /**
     * 🧱 解析單個元素
     */
    private ModelElement parseElement(JsonObject element) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");

        float x1 = from.get(0).getAsFloat() / 16.0F;
        float y1 = from.get(1).getAsFloat() / 16.0F;
        float z1 = from.get(2).getAsFloat() / 16.0F;
        float x2 = to.get(0).getAsFloat() / 16.0F;
        float y2 = to.get(1).getAsFloat() / 16.0F;
        float z2 = to.get(2).getAsFloat() / 16.0F;

        // 解析旋轉
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

        // 解析 UV
        Map<String, FaceUV> faceUVs = new HashMap<>();
        if (element.has("faces")) {
            JsonObject faces = element.getAsJsonObject("faces");
            for (String faceName : faces.keySet()) {
                JsonObject face = faces.getAsJsonObject(faceName);
                if (face.has("uv")) {
                    JsonArray uv = face.getAsJsonArray("uv");
                    float u1 = uv.get(0).getAsFloat() / 16.0F;
                    float v1 = uv.get(1).getAsFloat() / 16.0F;
                    float u2 = uv.get(2).getAsFloat() / 16.0F;
                    float v2 = uv.get(3).getAsFloat() / 16.0F;
                    faceUVs.put(faceName, new FaceUV(u1, v1, u2, v2));
                }
            }
        }

        return new ModelElement(x1, y1, z1, x2, y2, z2, rotationAngle, rotationAxis, rotationOrigin, faceUVs);
    }

    @Override
    public void render(ArcanePedestalBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // 🎨 渲染方塊模型
        if (modelLoaded) {
            renderBlockModel(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // 🎁 渲染祭品物品
        ItemStack offering = blockEntity.getOffering();
        if (!offering.isEmpty()) {
            renderOffering(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay, offering);
        }
    }

    /**
     * 🎨 渲染方塊模型
     */
    private void renderBlockModel(ArcanePedestalBlockEntity blockEntity, float partialTick,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay) {
        poseStack.pushPose();

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid(TEXTURE));

        // 🧱 渲染所有群組
        for (Map.Entry<String, List<ModelElement>> entry : groupElements.entrySet()) {
            renderGroup(poseStack, vertexConsumer, packedLight, packedOverlay, entry.getValue());
        }

        poseStack.popPose();
    }

    /**
     * 🎨 渲染群組元素
     */
    private void renderGroup(PoseStack poseStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay, List<ModelElement> elements) {
        for (ModelElement element : elements) {
            renderElement(poseStack, vertexConsumer, packedLight, packedOverlay, element);
        }
    }

    /**
     * 🧱 渲染單個元素
     */
    private void renderElement(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, ModelElement element) {
        poseStack.pushPose();

        // 🔄 處理旋轉
        if (element.rotationAngle != 0) {
            poseStack.translate(element.rotationOrigin[0], element.rotationOrigin[1], element.rotationOrigin[2]);
            switch (element.rotationAxis) {
                case "x" -> poseStack.mulPose(Axis.XP.rotationDegrees(element.rotationAngle));
                case "y" -> poseStack.mulPose(Axis.YP.rotationDegrees(element.rotationAngle));
                case "z" -> poseStack.mulPose(Axis.ZP.rotationDegrees(element.rotationAngle));
            }
            poseStack.translate(-element.rotationOrigin[0], -element.rotationOrigin[1], -element.rotationOrigin[2]);
        }

        // 🧊 渲染立方體
        renderCube(poseStack, vertexConsumer, packedLight, packedOverlay, element);

        poseStack.popPose();
    }

    /**
     * 🧊 渲染立方體
     */
    private void renderCube(PoseStack poseStack, VertexConsumer vertexConsumer,
                            int packedLight, int packedOverlay, ModelElement element) {
        Matrix4f matrix = poseStack.last().pose();

        float x1 = element.x1, y1 = element.y1, z1 = element.z1;
        float x2 = element.x2, y2 = element.y2, z2 = element.z2;

        // 渲染6個面
        FaceUV topUV = element.faceUVs.getOrDefault("up", new FaceUV(0, 0, 1, 1));
        addQuad(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1,
                0, 1, 0, topUV);

        FaceUV bottomUV = element.faceUVs.getOrDefault("down", new FaceUV(0, 0, 1, 1));
        addQuad(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
                0, -1, 0, bottomUV);

        FaceUV northUV = element.faceUVs.getOrDefault("north", new FaceUV(0, 0, 1, 1));
        addQuad(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1,
                0, 0, -1, northUV);

        FaceUV southUV = element.faceUVs.getOrDefault("south", new FaceUV(0, 0, 1, 1));
        addQuad(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
                0, 0, 1, southUV);

        FaceUV westUV = element.faceUVs.getOrDefault("west", new FaceUV(0, 0, 1, 1));
        addQuad(vertexConsumer, matrix, packedLight, packedOverlay,
                x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
                -1, 0, 0, westUV);

        FaceUV eastUV = element.faceUVs.getOrDefault("east", new FaceUV(0, 0, 1, 1));
        addQuad(vertexConsumer, matrix, packedLight, packedOverlay,
                x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2,
                1, 0, 0, eastUV);
    }

    /**
     * 📐 添加四邊形
     */
    private void addQuad(VertexConsumer vertexConsumer, Matrix4f matrix, int packedLight, int packedOverlay,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         float x3, float y3, float z3, float x4, float y4, float z4,
                         float nx, float ny, float nz, FaceUV faceUV) {
        int color = -1;
        vertexConsumer.addVertex(matrix, x1, y1, z1).setColor(color).setUv(faceUV.u1, faceUV.v2).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        vertexConsumer.addVertex(matrix, x2, y2, z2).setColor(color).setUv(faceUV.u1, faceUV.v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        vertexConsumer.addVertex(matrix, x3, y3, z3).setColor(color).setUv(faceUV.u2, faceUV.v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        vertexConsumer.addVertex(matrix, x4, y4, z4).setColor(color).setUv(faceUV.u2, faceUV.v2).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
    }

    /**
     * 🎁 渲染祭品物品
     */
    private void renderOffering(ArcanePedestalBlockEntity blockEntity, float partialTick,
                                PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay, ItemStack offering) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.0D, 0.5D); // 物品在方塊頂部

        float hoverOffset = blockEntity.getHoverOffset(partialTick);
        poseStack.translate(0.0D, hoverOffset, 0.0D);

        float rotation = blockEntity.getSpinForRender(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        if (blockEntity.isOfferingConsumed()) {
            float pulse = 0.3f + 0.2f * (float) Math.sin((blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L) / 5.0f);
            float consumedScale = 0.8f + pulse * 0.05f;
            poseStack.scale(consumedScale, consumedScale, consumedScale);
        }

        poseStack.scale(0.5f, 0.5f, 0.5f);

        // 使用方塊位置的實際光照值
        int light = getLightLevel(blockEntity.getLevel(), blockEntity.getBlockPos());
        itemRenderer.renderStatic(
                offering,
                ItemDisplayContext.GROUND,
                light,
                packedOverlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
        );

        poseStack.popPose();
    }

    /**
     * 取得方塊位置的光照等級
     */
    private int getLightLevel(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (level == null) {
            return 0xF000F0; // 預設最大光照
        }
        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        return net.minecraft.client.renderer.LightTexture.pack(blockLight, skyLight);
    }

    // === 數據類 ===

    private static class ModelElement {
        final float x1, y1, z1, x2, y2, z2;
        final float rotationAngle;
        final String rotationAxis;
        final float[] rotationOrigin;
        final Map<String, FaceUV> faceUVs;

        ModelElement(float x1, float y1, float z1, float x2, float y2, float z2,
                     float rotationAngle, String rotationAxis, float[] rotationOrigin,
                     Map<String, FaceUV> faceUVs) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.rotationAngle = rotationAngle;
            this.rotationAxis = rotationAxis;
            this.rotationOrigin = rotationOrigin;
            this.faceUVs = faceUVs;
        }
    }

    private static class FaceUV {
        final float u1, v1, u2, v2;

        FaceUV(float u1, float v1, float u2, float v2) {
            this.u1 = u1; this.v1 = v1;
            this.u2 = u2; this.v2 = v2;
        }
    }
}
