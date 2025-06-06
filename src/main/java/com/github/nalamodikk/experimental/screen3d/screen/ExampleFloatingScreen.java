    package com.github.nalamodikk.experimental.screen3d.screen;

    import com.github.nalamodikk.KoniavacraftMod;
    import com.github.nalamodikk.experimental.screen3d.api.FloatingScreen;
    import com.github.nalamodikk.experimental.screen3d.util.GuiQuadBatcher;
    import com.mojang.blaze3d.vertex.PoseStack;
    import com.mojang.blaze3d.vertex.VertexConsumer;
    import net.minecraft.client.Camera;
    import net.minecraft.client.renderer.MultiBufferSource;
    import net.minecraft.client.renderer.RenderType;
    import net.minecraft.core.BlockPos;
    import net.minecraft.resources.ResourceLocation;
    import net.minecraft.util.Mth;
    import net.minecraft.world.phys.Vec3;
    public class ExampleFloatingScreen extends FloatingScreen {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "textures/gui/modular_panel.png");
        private final BlockPos anchorPos;

        public ExampleFloatingScreen( BlockPos anchorPos, float width, float height) {
            super(Vec3.ZERO, width, height);
            this.anchorPos = anchorPos;
            this.scale = 10.0f;
            this.alpha = 1.0f;

        }

        @Override
        public void render(PoseStack stack, MultiBufferSource buffer, Camera camera, float partialTick) {
            Vec3 camPos = camera.getPosition();
            stack.pushPose();
            if (autoScale) {
                double distance = position.distanceToSqr(camera.getPosition());
                scale = Mth.clamp((float)(6.0 / Math.sqrt(distance)), 0.2f, 2.0f);
            }

            stack.translate(position.x - camPos.x, position.y - camPos.y, position.z - camPos.z);
            stack.mulPose(camera.rotation());
            stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
            stack.scale(scale, scale, scale);

            VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            GuiQuadBatcher.putQuad(consumer, stack.last().pose(), -width / 2, -height / 2, width, height, alpha);

            stack.popPose();
        }


        @Override
        public void tick() {
            // ✅ 每 tick 更新為方塊上方（例如方塊中心 + 高度偏移）
            this.position = Vec3.atCenterOf(anchorPos).add(0, 1.5, 0); // 上方 1.5 格
        }
    }

