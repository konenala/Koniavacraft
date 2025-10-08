package com.github.nalamodikk.common.block.blockentity.ritual.structure;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 儀式結構藍圖註冊中心。
 */
public final class RitualStructureBlueprintRegistry {

    private static final Map<ResourceLocation, RitualStructureBlueprint> BLUEPRINTS = new ConcurrentHashMap<>();

    private RitualStructureBlueprintRegistry() {
    }

    public static void register(RitualStructureBlueprint blueprint) {
        BLUEPRINTS.put(blueprint.id(), blueprint);
    }

    public static Optional<RitualStructureBlueprint> get(ResourceLocation id) {
        return Optional.ofNullable(BLUEPRINTS.get(id));
    }

    public static Map<ResourceLocation, RitualStructureBlueprint> all() {
        return Collections.unmodifiableMap(BLUEPRINTS);
    }
}
