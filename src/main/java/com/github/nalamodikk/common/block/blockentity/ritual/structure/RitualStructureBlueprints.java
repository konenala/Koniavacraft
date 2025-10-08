package com.github.nalamodikk.common.block.blockentity.ritual.structure;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 預設儀式結構藍圖集合。
 */
public final class RitualStructureBlueprints {

    public static final ResourceLocation DEFAULT_BLUEPRINT_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "ritual/default");

    static {
        RitualStructureBlueprintRegistry.register(createDefaultBlueprint());
    }

    private RitualStructureBlueprints() {
    }

    private static RitualStructureBlueprint createDefaultBlueprint() {
        List<StructureElement> elements = new ArrayList<>();

        // Core
        elements.add(new StructureElement(BlockPos.ZERO, StructureElementType.CORE, StructureRing.CORE));

        // Ring 1 (必備)
        ring(elements, StructureRing.RING1,
                new BlockPos(2, 0, 0),
                new BlockPos(-2, 0, 0),
                new BlockPos(0, 0, 2),
                new BlockPos(0, 0, -2)
        );

        // Ring 2 (對角線)
        ring(elements, StructureRing.RING2,
                new BlockPos(2, 0, 2),
                new BlockPos(2, 0, -2),
                new BlockPos(-2, 0, 2),
                new BlockPos(-2, 0, -2)
        );

        // Ring 3 (外圈)
        ring(elements, StructureRing.RING3,
                new BlockPos(4, 0, 0),
                new BlockPos(-4, 0, 0),
                new BlockPos(0, 0, 4),
                new BlockPos(0, 0, -4)
        );

        // 其他建議元素
        elements.add(new StructureElement(new BlockPos(3, 1, 3), StructureElementType.PYLON, StructureRing.EXTRA));
        elements.add(new StructureElement(new BlockPos(-3, 1, 3), StructureElementType.PYLON, StructureRing.EXTRA));

        elements.add(new StructureElement(new BlockPos(3, 0, 1), StructureElementType.RUNE, StructureRing.EXTRA));
        elements.add(new StructureElement(new BlockPos(-3, 0, 1), StructureElementType.RUNE, StructureRing.EXTRA));
        elements.add(new StructureElement(new BlockPos(3, 0, -1), StructureElementType.RUNE, StructureRing.EXTRA));
        elements.add(new StructureElement(new BlockPos(-3, 0, -1), StructureElementType.RUNE, StructureRing.EXTRA));

        elements.add(new StructureElement(new BlockPos(1, 0, 1), StructureElementType.GLYPH, StructureRing.EXTRA));
        elements.add(new StructureElement(new BlockPos(-1, 0, 1), StructureElementType.GLYPH, StructureRing.EXTRA));
        elements.add(new StructureElement(new BlockPos(1, 0, -1), StructureElementType.GLYPH, StructureRing.EXTRA));
        elements.add(new StructureElement(new BlockPos(-1, 0, -1), StructureElementType.GLYPH, StructureRing.EXTRA));

        return new SimpleRitualStructureBlueprint(DEFAULT_BLUEPRINT_ID, elements);
    }

    private static void ring(List<StructureElement> elements, StructureRing ring, BlockPos... offsets) {
        for (BlockPos offset : offsets) {
            elements.add(new StructureElement(offset, StructureElementType.PEDESTAL, ring));
        }
    }

    /**
     * 基礎實作。
     */
    private record SimpleRitualStructureBlueprint(ResourceLocation id, List<StructureElement> elements)
            implements RitualStructureBlueprint {
    }
}
