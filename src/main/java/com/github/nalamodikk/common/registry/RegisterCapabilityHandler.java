package com.github.nalamodikk.common.registry;


import com.github.nalamodikk.common.capability.IUnifiedManaHandler;
import com.github.nalamodikk.common.capability.ManaCapability;
import com.github.nalamodikk.common.block.TileEntity.mana_crafting.ManaCraftingTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RegisterCapabilityHandler {

    public static void register() {
        // 註冊到 MinecraftForge 事件總線
        MinecraftForge.EVENT_BUS.register(new RegisterCapabilityHandler());
    }

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof ManaCraftingTableBlockEntity blockEntity) {
            // 判斷是否已經具有 MANA 能力，避免重複附加
            LazyOptional<IUnifiedManaHandler> manaCap = blockEntity.getCapability(ManaCapability.MANA);

        }
    }
}
