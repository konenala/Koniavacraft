package com.github.nalamodikk.common.screen.manacollector;

import com.github.nalamodikk.common.block.TileEntity.basic.SolarManaCollectorBlockEntity;
import com.github.nalamodikk.common.capability.ManaCapability;
import com.github.nalamodikk.common.registry.ModMenusTypes;
import com.github.nalamodikk.common.sync.MachineSyncManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;


public class SolarManaCollectorMenu extends AbstractContainerMenu {
    private final SolarManaCollectorBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    private final MachineSyncManager syncManager;
    private final DataSlot manaStored = DataSlot.standalone();
    private final DataSlot maxMana = DataSlot.standalone();
    private int uiCachedMana; // 只存在 client，用來顯示 UI
    private int uiMaxMana;


    public SolarManaCollectorMenu(int id, Inventory inv, SolarManaCollectorBlockEntity blockEntity) {
        super(ModMenusTypes.SOLAR_MANA_COLLECTOR_MENU.get(), id);

        if (blockEntity == null || blockEntity.getLevel() == null) {
            throw new IllegalStateException("SolarManaCollectorBlockEntity 或其 Level 為 null");
        }

        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        this.syncManager = new MachineSyncManager();

        // --- 客製的 DataSlot --- （這是你 menu 用來畫面顯示用的）
        this.addDataSlot(new DataSlot() {
            @Override public int get() {
                return blockEntity.getManaStored();
            }

            @Override public void set(int value) {
                uiCachedMana = value; // 這應該是一個私有欄位
            }

        });
        this.addDataSlot(new DataSlot() {
            @Override public int get() {
                return blockEntity.getMaxMana();
            }

            @Override public void set(int value) {
                uiMaxMana = value;
            }
        });


        // --- Capability 同步部分 ---
        blockEntity.getCapability(ManaCapability.MANA).ifPresent(mana -> {
            syncManager.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return mana.getManaStored();
                }

                @Override
                public void set(int value) {
                    // 客戶端收到，通常不需要設定
                }
            });

            syncManager.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return mana.getMaxManaStored();
                }

                @Override
                public void set(int value) {}
            });
        });

        this.addDataSlots(syncManager);

        // --- 插入玩家物品欄 ---
        this.addPlayerInventorySlots(inv, 8, 84);
        this.addPlayerHotbarSlots(inv, 8, 142);
    }


    public int getManaStored() {
        return uiCachedMana; // 提供給 Screen 使用的值
    }
    public int getMaxMana() {
        return uiMaxMana;
    }
    private final DataSlot generatingSlot = new DataSlot() {
        @Override
        public int get() {
            return blockEntity.isCurrentlyGenerating() ? 1 : 0;
        }

        @Override
        public void set(int value) {
            blockEntity.setCurrentlyGenerating(value != 0);
        }
    };




    public void addPlayerInventorySlots(Inventory inv, int startX, int startY) {
        // 玩家主背包 (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int index = col + row * 9 + 9;
                int x = startX + col * 18;
                int y = startY + row * 18;
                this.addSlot(new Slot(inv, index, x, y));
            }
        }
    }

    public void addPlayerHotbarSlots(Inventory inv, int startX, int startY) {
        // 玩家快捷欄 (1x9)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, startX + col * 18, startY));
        }
    }

    public SolarManaCollectorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (SolarManaCollectorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }


    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, blockEntity.getBlockState().getBlock());
    }


    public SolarManaCollectorBlockEntity getBlockEntity() {
        return blockEntity;
    }


    public DataSlot getGeneratingSlot() {
        return generatingSlot;
    }
}
