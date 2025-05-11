package com.github.nalamodikk.common.ComponentSystem.register.component;

import com.github.nalamodikk.common.ComponentSystem.API.machine.component.ManaStorageComponent;
import com.github.nalamodikk.common.MagicalIndustryMod;
import net.minecraft.resources.ResourceLocation;

/**
 * 用來註冊所有模組零件，例如 mana_core、magic_pump、原能感應器等。
 * 在 FMLCommonSetupEvent 中呼叫 registerAll() 即可。
 */
public class ModComponents {

    public static void registerAll() {
        // 🔧 註冊 mana_core 模組
        // ComponentRegistry.register(new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_core"), ManaCoreComponent::new);
        ComponentRegistry.register(new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_storage"), ManaStorageComponent::new);
        // ✨ 你未來還可以繼續加下去，例如：
        // ComponentRegistry.register(new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_pump"), ManaPumpComponent::new);
        // ComponentRegistry.register(new ResourceLocation(MagicalIndustryMod.MOD_ID, "magic_output"), MagicOutputComponent::new);
    }

    private ModComponents() {}
}
