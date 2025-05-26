package com.github.nalamodikk.common.capability;

import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

public class ManaCapability {
    public static final BlockCapability<IUnifiedManaHandler, Void> MANA =
            BlockCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath(MagicalIndustryMod.MOD_ID, "mana"),
                    IUnifiedManaHandler.class
            );
}




