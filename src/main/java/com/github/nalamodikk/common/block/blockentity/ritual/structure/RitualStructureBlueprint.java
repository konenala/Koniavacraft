package com.github.nalamodikk.common.block.blockentity.ritual.structure;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 儀式結構藍圖介面。
 * 提供 JEI 顯示與世界投影所需的結構元素資訊。
 */
public interface RitualStructureBlueprint {

    /**
     * @return 藍圖對應的資源識別碼。
     */
    ResourceLocation id();

    /**
     * @return 此藍圖包含的結構元素清單。
     */
    List<StructureElement> elements();
}
