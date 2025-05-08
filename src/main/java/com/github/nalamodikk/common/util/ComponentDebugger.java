package com.github.nalamodikk.common.util;

import com.github.nalamodikk.common.API.machine.IGridComponent;
import com.github.nalamodikk.common.API.machine.grid.ComponentGrid;
import com.github.nalamodikk.common.API.machine.grid.ComponentRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ComponentDebugger {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void exportToFile(ComponentGrid grid, MinecraftServer server, String fileName) {
        JsonObject data = exportGridAsJson(grid);
        try {
            FileWriter writer = new FileWriter(server.getWorldPath(null).resolve(fileName).toFile());
            GSON.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("寫入拼裝資料到 " + fileName + " 時失敗", e);
        }
    }

    /**
     * 將 ComponentGrid 中的所有元件匯出為 JSON 結構
     */
    public static JsonObject exportGridAsJson(ComponentGrid grid) {
        JsonObject root = new JsonObject();
        JsonArray components = new JsonArray();

        for (Map.Entry<BlockPos, IGridComponent> entry : grid.getAllComponents().entrySet()) {
            BlockPos pos = entry.getKey();
            IGridComponent comp = entry.getValue();

            ComponentRecord record = new ComponentRecord(pos, comp.getId(), comp.getData());

            JsonObject compJson = new JsonObject();
            compJson.addProperty("x", pos.getX());
            compJson.addProperty("y", pos.getZ()); // Y 軸為 Grid 的 Z 軸
            compJson.addProperty("id", comp.getId().toString());
            compJson.add("data", NBTJsonConverter.convert(record.data()));

            components.add(compJson);
        }

        root.add("components", components);
        return root;
    }
}
