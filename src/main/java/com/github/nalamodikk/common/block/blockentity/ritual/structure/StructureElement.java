package com.github.nalamodikk.common.block.blockentity.ritual.structure;

import net.minecraft.core.BlockPos;

/**
 * 儀式結構中的單一元素，包含相對座標與類型等資訊。
 */
public record StructureElement(BlockPos offset, StructureElementType type, StructureRing ring) {

    public StructureElement {
        if (offset == null) {
            throw new IllegalArgumentException("offset cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (ring == null) {
            throw new IllegalArgumentException("ring cannot be null");
        }
    }
}
