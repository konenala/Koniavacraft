package com.github.nalamodikk.common.ComponentSystem.API.machine.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public record ComponentRecord(BlockPos pos, ResourceLocation id, CompoundTag data) {

    public static @Nullable ComponentRecord fromNBT(CompoundTag tag) {
        if (!tag.contains("x") || !tag.contains("y") || !tag.contains("id")) return null;

        int x = tag.getInt("x");
        int y = tag.getInt("y");
        BlockPos pos = new BlockPos(x, 0, y);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        if (id == null) return null;

        CompoundTag data = tag.getCompound("data");
        return new ComponentRecord(pos, id, data);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getZ());
        tag.putString("id", id.toString());
        tag.put("data", data);
        return tag;
    }
}
