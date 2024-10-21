package com.github.nalamodikk;

import com.github.nalamodikk.block.ModBlocks;
import com.github.nalamodikk.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import com.github.nalamodikk.block.entity.ModBlockEntities;
import com.github.nalamodikk.Capability.ModCapabilities;  // 新增的导入
import com.github.nalamodikk.item.ModCreativeModTabs;
import com.github.nalamodikk.item.ModItems;
import com.github.nalamodikk.recipe.ModRecipes;
import com.github.nalamodikk.screen.ManaCrafting.AdvancedManaCraftingTableScreen;
import com.github.nalamodikk.screen.ManaCrafting.ManaCraftingScreen;
import com.github.nalamodikk.screen.ModMenusTypes;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent; // 用于附加 Capability
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MagicalIndustryMod.MOD_ID)
public class MagicalIndustryMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "magical_industry";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();


    public MagicalIndustryMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册创造模式标签
        ModCreativeModTabs.register(modEventBus);

        // 注册物品和方块
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        modEventBus.register(ModCapabilities.class);

        // 注册菜单类型和方块实体和配方
        ModMenusTypes.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModRecipes.register(modEventBus);

        // 注册模组的生命周期事件
        modEventBus.addListener(this::commonSetup);

        // 注册创造模式标签的内容
        modEventBus.addListener(this::addCreative);

        // 注册 MinecraftForge 的事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 通用设置

    }


    // 添加物品到创造模式标签
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 在这里添加物品到相应的创造模式标签
    }

    // 服务器启动事件
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    // 附加 Capability 给 ManaCraftingTableBlockEntity
    // 附加 Capability 给 ManaCraftingTableBlockEntity
    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof ManaCraftingTableBlockEntity blockEntity) {
            event.addCapability(new ResourceLocation(MOD_ID, "mana"), new ManaCraftingTableBlockEntity.Provider(blockEntity));
        }
    }


    // 客户端事件订阅器
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // 客户端设置
            MenuScreens.register(ModMenusTypes.MANA_CRAFTING_MENU.get(), ManaCraftingScreen::new);
            MenuScreens.register(ModMenusTypes.ADVANCED_MANA_CRAFTING_TABLE_MENu.get(), AdvancedManaCraftingTableScreen::new);        }
    }
}
