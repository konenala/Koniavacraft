package com.github.nalamodikk.client.screenAPI.gui.api;

import net.minecraftforge.items.IItemHandler;

public interface IGuiDataContext {
    <T> T query(String key);
    Object host(); // ex: BlockEntity, Screen, etc.
    IItemHandler getItemHandler(); // ✅ 提供模組物品儲存槽

}
