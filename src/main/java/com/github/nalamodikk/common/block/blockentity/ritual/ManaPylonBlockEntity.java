package com.github.nalamodikk.common.block.blockentity.ritual;

import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔力塔方塊實體 - 儀式系統的魔力供應器
 * 負責：
 * 1. 從相鄰魔力導管網絡抽取魔力
 * 2. 為儀式提供大容量魔力儲存
 * 3. 管理魔力輸入輸出
 * 4. 提供視覺化的魔力狀態
 */
public class ManaPylonBlockEntity extends BlockEntity {
    
    private static final int MAX_CAPACITY = 500000; // 500K 魔力容量
    private static final int MAX_TRANSFER_RATE = 10000; // 每tick最大傳輸量
    
    private final ManaStorage manaStorage;
    private final List<BlockCapabilityCache<IUnifiedManaHandler, Direction>> manaCapabilityCaches;
    
    private boolean isConnectedToNetwork = false;
    private int transferCooldown = 0;
    
    // 渲染相關
    private float crystalGlow = 0.0f;
    private float energyAnimation = 0.0f;
    private int tickCount = 0;
    
    public ManaPylonBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MANA_PYLON_BE.get(), pos, blockState);
        this.manaStorage = new ManaStorage(MAX_CAPACITY);
        this.manaCapabilityCaches = new ArrayList<>();
        
        // 為所有六個方向創建空的能力緩存列表，將在setLevel時初始化
        for (int i = 0; i < 6; i++) {
            manaCapabilityCaches.add(null);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        // 更新所有緩存的世界引用
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < manaCapabilityCaches.size(); i++) {
                Direction dir = Direction.values()[i];
                manaCapabilityCaches.set(i, BlockCapabilityCache.create(
                    ModCapabilities.MANA,
                    serverLevel,
                    worldPosition.relative(dir),
                    dir.getOpposite()
                ));
            }
        }
    }

    public void tick() {
        if (level == null) return;
        
        tickCount++;
        
        // 客戶端渲染動畫
        if (level.isClientSide()) {
            updateRenderingEffects();
        }
        
        // 服務端邏輯
        if (!level.isClientSide()) {
            // 減少傳輸冷卻時間
            if (transferCooldown > 0) {
                transferCooldown--;
            }
            
            // 嘗試從網絡中抽取魔力
            if (transferCooldown == 0) {
                attemptManaExtraction();
                transferCooldown = 5; // 5 tick冷卻
            }
            
            // 檢查網絡連接狀態
            updateNetworkConnection();
        }
    }

    /**
     * 更新渲染效果
     */
    private void updateRenderingEffects() {
        // 水晶發光效果（基於魔力存儲量）
        float fillRatio = (float) manaStorage.getManaStored() / manaStorage.getMaxManaStored();
        crystalGlow = 0.3f + (fillRatio * 0.7f);
        
        // 能量動畫
        energyAnimation += 0.05f;
        if (energyAnimation >= Math.PI * 2) {
            energyAnimation = 0.0f;
        }
    }

    /**
     * 嘗試從相鄰方塊抽取魔力
     */
    private void attemptManaExtraction() {
        if (manaStorage.getManaStored() >= manaStorage.getMaxManaStored()) {
            return; // 已滿
        }
        
        int totalExtracted = 0;
        boolean foundConnection = false;
        
        for (BlockCapabilityCache<IUnifiedManaHandler, Direction> cache : manaCapabilityCaches) {
            IUnifiedManaHandler handler = cache.getCapability();
            if (handler != null) {
                foundConnection = true;
                
                // 嘗試抽取魔力
                int neededMana = Math.min(
                    MAX_TRANSFER_RATE - totalExtracted,
                    manaStorage.getMaxManaStored() - manaStorage.getManaStored()
                );
                
                if (neededMana > 0) {
                    int extracted = handler.extractMana(neededMana, ManaAction.EXECUTE);
                    if (extracted > 0) {
                        manaStorage.receiveMana(extracted, ManaAction.EXECUTE);
                        totalExtracted += extracted;
                        
                        if (totalExtracted >= MAX_TRANSFER_RATE) {
                            break; // 達到最大傳輸速率
                        }
                    }
                }
            }
        }
        
        isConnectedToNetwork = foundConnection;
        
        if (totalExtracted > 0) {
            setChanged();
        }
    }

    /**
     * 更新網絡連接狀態
     */
    private void updateNetworkConnection() {
        boolean wasConnected = isConnectedToNetwork;
        isConnectedToNetwork = false;
        
        for (BlockCapabilityCache<IUnifiedManaHandler, Direction> cache : manaCapabilityCaches) {
            if (cache.getCapability() != null) {
                isConnectedToNetwork = true;
                break;
            }
        }
        
        if (wasConnected != isConnectedToNetwork) {
            setChanged();
        }
    }

    /**
     * 為儀式提供魔力
     */
    public int provideManaForRitual(int amount, boolean simulate) {
        return manaStorage.extractMana(amount, simulate ? ManaAction.SIMULATE : ManaAction.EXECUTE);
    }

    /**
     * 檢查是否可以提供指定量的魔力
     */
    public boolean canProvideMana(int amount) {
        return manaStorage.getManaStored() >= amount;
    }

    /**
     * 當方塊被移除時調用
     */
    public void onRemoved() {
        // 清理緩存
        manaCapabilityCaches.clear();
    }

    // Getters
    public int getStoredMana() { 
        return manaStorage.getManaStored(); 
    }
    
    public int getMaxManaCapacity() { 
        return manaStorage.getMaxManaStored(); 
    }
    
    public boolean isConnectedToNetwork() { 
        return isConnectedToNetwork; 
    }
    
    public float getCrystalGlow() { 
        return crystalGlow; 
    }
    
    public float getEnergyAnimation() { 
        return energyAnimation; 
    }
    
    public ManaStorage getManaStorage() {
        return manaStorage;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        tag.put("ManaStorage", manaStorage.serializeNBT(registries));
        tag.putBoolean("IsConnected", isConnectedToNetwork);
        tag.putInt("TransferCooldown", transferCooldown);
        tag.putFloat("CrystalGlow", crystalGlow);
        tag.putFloat("EnergyAnimation", energyAnimation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        if (tag.contains("ManaStorage")) {
            manaStorage.deserializeNBT(registries, tag.getCompound("ManaStorage"));
        }
        
        isConnectedToNetwork = tag.getBoolean("IsConnected");
        transferCooldown = tag.getInt("TransferCooldown");
        crystalGlow = tag.getFloat("CrystalGlow");
        energyAnimation = tag.getFloat("EnergyAnimation");
    }
}