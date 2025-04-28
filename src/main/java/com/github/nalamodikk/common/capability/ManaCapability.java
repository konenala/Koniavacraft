package com.github.nalamodikk.common.capability;

import com.github.nalamodikk.common.capability.mana.IManaBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

public class ManaCapability {
    public static final Capability<IUnifiedManaHandler> MANA = CapabilityManager.get(new CapabilityToken<>() {});

    @Mod.EventBusSubscriber
    public static class EventHandler {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
            BlockEntity be = event.getObject();
            if (be instanceof IManaBlockEntity manaBlock) {
                if (!be.getCapability(ManaCapability.MANA).isPresent()) {
                    event.addCapability(
                            new ResourceLocation("magical_industry", "mana"),
                            new ManaProvider(manaBlock.getManaStorage())
                    );
                }
            }
        }
    }



}
