package com.github.nalamodikk.client.ritual;

import com.github.nalamodikk.common.block.blockentity.ritual.structure.RitualStructureBlueprint;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.RitualStructureBlueprintRegistry;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.StructureElement;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.StructureElementType;
import com.github.nalamodikk.common.block.blockentity.ritual.structure.StructureRing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 客戶端儀式結構投影管理器。
 */
public final class RitualStructureProjectionManager {

    private static final RitualStructureProjectionManager INSTANCE = new RitualStructureProjectionManager();

    private final EnumMap<StructureRing, Boolean> ringToggles = new EnumMap<>(StructureRing.class);
    private ActiveProjection activeProjection;

    private RitualStructureProjectionManager() {
        ringToggles.put(StructureRing.RING2, Boolean.FALSE);
        ringToggles.put(StructureRing.RING3, Boolean.FALSE);
    }

    public static RitualStructureProjectionManager getInstance() {
        return INSTANCE;
    }

    public boolean isRingEnabled(StructureRing ring) {
        if (ring == StructureRing.RING1 || ring == StructureRing.CORE || ring == StructureRing.EXTRA) {
            return true;
        }
        return ringToggles.getOrDefault(ring, Boolean.FALSE);
    }

    public void setRingEnabled(StructureRing ring, boolean enabled) {
        if (ring != StructureRing.RING2 && ring != StructureRing.RING3) {
            return;
        }
        ringToggles.put(ring, enabled);
        if (activeProjection != null) {
            activeProjection = activeProjection.withOptions(currentOptions());
        }
    }

    public void toggleProjection(RitualStructureBlueprint blueprint) {
        if (activeProjection != null && activeProjection.blueprintId.equals(blueprint.id())) {
            clearProjection(true);
            return;
        }
        startProjection(blueprint);
    }

    public Optional<ActiveProjection> getActiveProjection() {
        return Optional.ofNullable(activeProjection);
    }

    public void clearProjection(boolean notify) {
        if (activeProjection != null && notify) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.koniavacraft.ritual.projection.cleared"), true);
            }
        }
        activeProjection = null;
    }

    private void startProjection(RitualStructureBlueprint blueprint) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        Optional<BlockPos> anchorOptional = findAnchor(player);
        if (anchorOptional.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.koniavacraft.ritual.projection.no_anchor"), true);
            return;
        }

        BlockPos anchor = anchorOptional.get();
        Direction facing = player.getDirection();
        ProjectionOptions options = currentOptions();
        activeProjection = new ActiveProjection(blueprint.id(), anchor, facing, options);

        player.displayClientMessage(Component.translatable(
                "message.koniavacraft.ritual.projection.started",
                Component.literal("%d %d %d".formatted(anchor.getX(), anchor.getY(), anchor.getZ()))
        ), true);
    }

    private Optional<BlockPos> findAnchor(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() == HitResult.Type.BLOCK) {
            return Optional.of(blockHitResult.getBlockPos());
        }
        // If no block targeted, try short ray trace ahead of player
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(5.0));
        BlockHitResult trace = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (trace.getType() == HitResult.Type.BLOCK) {
            return Optional.of(trace.getBlockPos());
        }
        return Optional.empty();
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (activeProjection != null && (mc.level == null || mc.player == null)) {
            clearProjection(false);
        }
    }

    public void render(RenderLevelStageEvent event) {
        if (activeProjection == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        RitualStructureBlueprint blueprint = RitualStructureBlueprintRegistry.get(activeProjection.blueprintId)
                .orElse(null);
        if (blueprint == null) {
            clearProjection(false);
            return;
        }

        List<StructureElement> elements = filterElements(blueprint.elements(), activeProjection.options);
        if (elements.isEmpty()) {
            return;
        }

        var poseStack = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var bufferSource = mc.renderBuffers().bufferSource();
        var lineBuffer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        for (StructureElement element : elements) {
            BlockPos offsetRotated = rotate(element.offset(), activeProjection.direction);
            BlockPos worldPos = activeProjection.anchor.offset(offsetRotated);
            float[] color = getColorFor(element.type());
            double minX = worldPos.getX();
            double minY = worldPos.getY();
            double minZ = worldPos.getZ();
            double maxX = minX + 1;
            double maxY = minY + 1;
            double maxZ = minZ + 1;
            net.minecraft.client.renderer.LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    color[0],
                    color[1],
                    color[2],
                    1.0F
            );
        }

        poseStack.popPose();
        bufferSource.endBatch(net.minecraft.client.renderer.RenderType.lines());
    }

    private ProjectionOptions currentOptions() {
        return new ProjectionOptions(
                ringToggles.getOrDefault(StructureRing.RING2, Boolean.FALSE),
                ringToggles.getOrDefault(StructureRing.RING3, Boolean.FALSE)
        );
    }

    private List<StructureElement> filterElements(List<StructureElement> elements, ProjectionOptions options) {
        List<StructureElement> filtered = new ArrayList<>();
        for (StructureElement element : elements) {
            if (element.type() == StructureElementType.PEDESTAL) {
                if (element.ring() == StructureRing.RING2 && !options.includeRing2()) {
                    continue;
                }
                if (element.ring() == StructureRing.RING3 && !options.includeRing3()) {
                    continue;
                }
            }
            filtered.add(element);
        }
        return filtered;
    }

    private BlockPos rotate(BlockPos pos, Direction direction) {
        return switch (direction) {
            case NORTH -> pos;
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            default -> pos;
        };
    }

    private float[] getColorFor(StructureElementType type) {
        return switch (type) {
            case CORE -> new float[]{0.4F, 0.5F, 1.0F};
            case PEDESTAL -> new float[]{0.2F, 0.8F, 1.0F};
            case PYLON -> new float[]{0.95F, 0.3F, 0.95F};
            case RUNE -> new float[]{0.95F, 0.85F, 0.25F};
            case GLYPH -> new float[]{0.95F, 0.95F, 0.95F};
        };
    }

    private record ProjectionOptions(boolean includeRing2, boolean includeRing3) {
    }

    public record ActiveProjection(ResourceLocation blueprintId,
                                   BlockPos anchor,
                                   Direction direction,
                                   ProjectionOptions options) {

        ActiveProjection withOptions(ProjectionOptions newOptions) {
            return new ActiveProjection(blueprintId, anchor, direction, newOptions);
        }
    }
}
