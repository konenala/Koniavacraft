package com.github.nalamodikk.common.ComponentSystem.API.machine.grid;


import com.github.nalamodikk.common.ComponentSystem.API.machine.IGridComponent;
import net.minecraft.nbt.CompoundTag;

/**
 * 提供 component 快取簽名的基底類別。
 */
public abstract class BaseGridComponent implements IGridComponent {
    private CompoundTag stableDataCache = null;

    private String cachedSignature = null;

    @Override
    public final String getSignatureString() {
        if (cachedSignature == null) {
            cachedSignature = getId().toString() + "#" + getDataStableHash();
        }
        return cachedSignature;
    }


    @Override
    public final void invalidateSignatureCache() {
        cachedSignature = null;
    }

    /**
     * 對 getData() 的內容產生穩定 hash。
     * 子類別可以 override 做更穩定的序列化。
     */
    protected int getDataStableHash() {
        return getData().toString().hashCode();
    }

    @Override
    public void loadFromNBT(CompoundTag tag) {
        invalidateSignatureCache(); // 載入資料時記得清除快取
    }
}
