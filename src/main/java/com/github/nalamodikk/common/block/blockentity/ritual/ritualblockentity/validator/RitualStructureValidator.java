package com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.validator;

import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.ArcanePedestalBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.ManaPylonBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.RuneStoneBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblock.ArcanePedestalBlock;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblock.ChalkGlyphBlock;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblock.RuneType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 儀式結構驗證器：檢查基座、魔力塔與符文石是否符合基礎配置。
 */
public class RitualStructureValidator {

    public static final String STRUCTURE_KEY_PREFIX_PEDESTAL_DIRECTION = "pedestal.";
    public static final String STRUCTURE_KEY_PREFIX_GLYPH_COLOR = "glyph.color.";
    public static final String STRUCTURE_KEY_PREFIX_GLYPH_PATTERN = "glyph.pattern.";
    public static final String STRUCTURE_KEY_GLYPH_TOTAL = "glyph.total";


    public static final String STRUCTURE_KEY_PEDESTAL = "pedestal.total";
    public static final String STRUCTURE_KEY_PYLON = "pylon.total";

    private static final int PEDESTAL_SCAN_RADIUS = 6;
    private static final int PYLON_SCAN_RADIUS = 10;
    private static final int RUNE_SCAN_RADIUS = 8;
    private static final int GLYPH_SCAN_RADIUS = 8;
    private static final double MAX_PEDESTAL_HEIGHT_DELTA = 1.0;
    private static final double MAX_PYLON_DISTANCE = 9.0;

    private static final Set<BlockPos> REQUIRED_PEDESTAL_OFFSETS = Set.of(
            new BlockPos(2, 0, 0),
            new BlockPos(-2, 0, 0),
            new BlockPos(0, 0, 2),
            new BlockPos(0, 0, -2)
    );

    private static final Map<RuneType, Integer> REQUIRED_RUNES = Map.of(
            RuneType.EFFICIENCY, 1,
            RuneType.CELERITY, 1,
            RuneType.STABILITY, 1,
            RuneType.AUGMENTATION, 1
    );

    /**
     * 進行結構驗證。
     */
    public boolean validate(RitualValidationContext context) {
        Level level = context.level();
        BlockPos corePos = context.corePos();

        List<ArcanePedestalBlockEntity> pedestals = collectPedestals(level, corePos, PEDESTAL_SCAN_RADIUS);
        context.setPedestals(pedestals);
        EnumMap<Direction, Integer> pedestalDirectionCounts = new EnumMap<>(Direction.class);
        validatePedestalLayout(context, corePos, pedestals, pedestalDirectionCounts);

        List<ManaPylonBlockEntity> pylons = collectPylons(level, corePos, PYLON_SCAN_RADIUS);
        context.setPylons(pylons);
        validatePylons(context, corePos, pylons);

        List<RuneStoneBlockEntity> runeStones = collectRuneStones(level, corePos, RUNE_SCAN_RADIUS);
        context.setRuneStones(runeStones);
        Map<RuneType, Integer> runeSummary = summarizeRunes(runeStones);
        context.setRuneSummary(runeSummary);
        validateRunes(context, runeSummary);

        List<RitualValidationContext.ChalkGlyphInfo> chalkGlyphs = collectChalkGlyphs(level, corePos, GLYPH_SCAN_RADIUS);
        context.setChalkGlyphs(chalkGlyphs);
        Map<String, Integer> glyphSummary = validateAndSummarizeGlyphs(context, chalkGlyphs);

        Map<String, Integer> summary = new HashMap<>();
        summary.put(STRUCTURE_KEY_PEDESTAL, pedestals.size());
        pedestalDirectionCounts.forEach((direction, count) -> summary.put(
                STRUCTURE_KEY_PREFIX_PEDESTAL_DIRECTION + direction.getName(), count));
        summary.put(STRUCTURE_KEY_PYLON, pylons.size());
        summary.putAll(glyphSummary);
        runeSummary.forEach((type, count) -> summary.put("rune." + type.getName(), count));
        context.setStructureSummary(summary);

        return !context.hasErrors();
    }

    /**
     * 掃描核心周圍的粉筆符號。
     */
    private List<RitualValidationContext.ChalkGlyphInfo> collectChalkGlyphs(Level level, BlockPos center, int radius) {
        List<RitualValidationContext.ChalkGlyphInfo> result = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof ChalkGlyphBlock) {
                        ChalkGlyphBlock.ChalkColor color = state.getValue(ChalkGlyphBlock.COLOR);
                        ChalkGlyphBlock.GlyphPattern pattern = state.getValue(ChalkGlyphBlock.PATTERN);
                        result.add(new RitualValidationContext.ChalkGlyphInfo(cursor.immutable(), color, pattern));
                    }
                }
            }
        }
        return result;
    }

    private Map<String, Integer> validateAndSummarizeGlyphs(RitualValidationContext context,
                                                            List<RitualValidationContext.ChalkGlyphInfo> glyphs) {
        Map<String, Integer> summary = new HashMap<>();

        if (glyphs.isEmpty()) {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.no_glyph"));
            return summary;
        }

        summary.put(STRUCTURE_KEY_GLYPH_TOTAL, glyphs.size());
        Map<String, Integer> colorCounts = new HashMap<>();
        Map<String, Integer> patternCounts = new HashMap<>();
        for (RitualValidationContext.ChalkGlyphInfo glyph : glyphs) {
            colorCounts.merge(glyph.color().getSerializedName(), 1, Integer::sum);
            patternCounts.merge(glyph.pattern().getSerializedName(), 1, Integer::sum);
        }
        colorCounts.forEach((color, count) ->
                summary.put(STRUCTURE_KEY_PREFIX_GLYPH_COLOR + color, count));

        patternCounts.forEach((pattern, count) ->
                summary.put(STRUCTURE_KEY_PREFIX_GLYPH_PATTERN + pattern, count));
        return summary;
    }



    /**
     * 掃描核心周圍的基座。
     */

    private List<ArcanePedestalBlockEntity> collectPedestals(Level level, BlockPos center, int radius) {
        List<ArcanePedestalBlockEntity> result = new ArrayList<>();
        scan(level, center, radius, (pos, entity) -> {
            if (entity instanceof ArcanePedestalBlockEntity pedestal) {
                result.add(pedestal);
            }
        });
        return result;
    }

    /**
     * 掃描核心周圍的魔力塔。
     */
    private List<ManaPylonBlockEntity> collectPylons(Level level, BlockPos center, int radius) {
        List<ManaPylonBlockEntity> result = new ArrayList<>();
        scan(level, center, radius, (pos, entity) -> {
            if (entity instanceof ManaPylonBlockEntity pylon) {
                result.add(pylon);
            }
        });
        return result;
    }

    /**
     * 掃描核心周圍的符文石。
     */
    private List<RuneStoneBlockEntity> collectRuneStones(Level level, BlockPos center, int radius) {
        List<RuneStoneBlockEntity> result = new ArrayList<>();
        scan(level, center, radius, (pos, entity) -> {
            if (entity instanceof RuneStoneBlockEntity runeStone) {
                result.add(runeStone);
            }
        });
        return result;
    }

    private Direction expectedDirectionForRelative(BlockPos relative) {
        int x = relative.getX();
        int z = relative.getZ();
        if (x == 2 && z == 0) {
            return Direction.WEST;
        }
        if (x == -2 && z == 0) {
            return Direction.EAST;
        }
        if (x == 0 && z == 2) {
            return Direction.NORTH;
        }
        if (x == 0 && z == -2) {
            return Direction.SOUTH;
        }
        return null;
    }

    /**
     * 檢查基座是否位於預期的四個方向並保持高度一致。
     */
    private void validatePedestalLayout(RitualValidationContext context, BlockPos corePos,
                                         List<ArcanePedestalBlockEntity> pedestals,
                                         Map<Direction, Integer> directionCounts) {
        if (pedestals.size() < REQUIRED_PEDESTAL_OFFSETS.size()) {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.no_pedestal"));
            return;
        }

        EnumSet<Direction> matchedDirections = EnumSet.noneOf(Direction.class);
        boolean facingMismatch = false;

        for (ArcanePedestalBlockEntity pedestal : pedestals) {
            BlockPos pos = pedestal.getBlockPos();
            BlockPos relative = pos.subtract(corePos);

            if (Math.abs(relative.getY()) > MAX_PEDESTAL_HEIGHT_DELTA) {
                context.addError(Component.translatable("message.koniavacraft.ritual.error.pedestal_height"));
                return;
            }

            Direction expected = expectedDirectionForRelative(relative);
            if (expected != null) {
                matchedDirections.add(expected);
                directionCounts.merge(expected, 1, Integer::sum);

                BlockState state = pedestal.getBlockState();
                if (state.getBlock() instanceof ArcanePedestalBlock) {
                    Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                    if (facing != expected) {
                        facingMismatch = true;
                    }
                } else {
                    facingMismatch = true;
                }
            }
        }

        if (matchedDirections.size() < REQUIRED_PEDESTAL_OFFSETS.size()) {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.pedestal_layout"));
        } else if (facingMismatch) {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.pedestal_facing"));
        }
    }

    /**
     * 檢查魔力塔距離是否符合需求。
     */
    private void validatePylons(RitualValidationContext context, BlockPos corePos, List<ManaPylonBlockEntity> pylons) {
        if (pylons.isEmpty()) {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.no_pylon"));
            return;
        }

        boolean ok = pylons.stream().anyMatch(pylon -> {
            double distance = Math.sqrt(pylon.getBlockPos().distSqr(corePos));
            return distance <= MAX_PYLON_DISTANCE;
        });

        if (!ok) {
            context.addError(Component.translatable("message.koniavacraft.ritual.error.pylon_distance", (int) MAX_PYLON_DISTANCE));
        }
    }

    /**
     * 統計每種符文石的數量。
     */
    private Map<RuneType, Integer> summarizeRunes(List<RuneStoneBlockEntity> runeStones) {
        Map<RuneType, Integer> counts = new EnumMap<>(RuneType.class);
        for (RuneStoneBlockEntity runeStone : runeStones) {
            RuneType type = runeStone.getRuneType();
            counts.merge(type, 1, Integer::sum);
        }
        return counts;
    }

    /**
     * 檢查符文石數量是否達到配方需求。
     */
    private void validateRunes(RitualValidationContext context, Map<RuneType, Integer> runeCounts) {
        for (Map.Entry<RuneType, Integer> entry : REQUIRED_RUNES.entrySet()) {
            RuneType type = entry.getKey();
            int required = entry.getValue();
            int actual = runeCounts.getOrDefault(type, 0);
            if (actual < required) {
                context.addError(Component.translatable(
                        "message.koniavacraft.ritual.error.rune_requirement",
                        Component.translatable(type.getTranslationKey()),
                        required
                ));
            }
        }
    }

    /**
     * 通用掃描方法，於指定半徑內遍歷方塊實體。
     */
    private void scan(Level level, BlockPos center, int radius, ScanConsumer consumer) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var entity = level.getBlockEntity(cursor);
                    if (entity != null) {
                        consumer.accept(cursor.immutable(), entity);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface ScanConsumer {
        void accept(BlockPos pos, Object blockEntity);
    }
}
