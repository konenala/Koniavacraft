package com.github.nalamodikk.common.utils;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class NBTJsonConverter {
    public static JsonObject convert(CompoundTag tag) {
        JsonObject json = new JsonObject();
        for (String key : tag.getAllKeys()) {
            Tag value = tag.get(key);
            if (value != null) {
                json.addProperty(key, value.getAsString()); // 簡單轉字串
            }
        }
        return json;
    }
}
