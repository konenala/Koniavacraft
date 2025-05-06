package com.github.nalamodikk.common;

import com.github.nalamodikk.common.register.ModBlocks;
import com.github.nalamodikk.common.register.ModBlockEntities;
import com.github.nalamodikk.common.capability.ModCapabilities;  // 新增的导入
import com.github.nalamodikk.common.register.ModCreativeModTabs;
import com.github.nalamodikk.common.register.ModItems;
import com.github.nalamodikk.common.network.handler.NetworkHandler;
import com.github.nalamodikk.common.register.ModRecipes;
import com.github.nalamodikk.common.register.*;
import com.github.nalamodikk.common.register.ModMenusTypes;
import com.github.nalamodikk.common.recipe.fuel.loader.FuelRateLoader;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MagicalIndustryMod.MOD_ID)
public class MagicalIndustryMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "magical_industry";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();


    public MagicalIndustryMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ConfigManager.registerConfigs();

        GeckoLib.initialize();
        // 注册创造模式标签
        ModCreativeModTabs.register(modEventBus);
        LOGGER.debug("這是 Debug 訊息");
        LOGGER.info("這是 Info 訊息");
        LOGGER.warn("這是 Warn 訊息");
        LOGGER.error("這是 Error 訊息");

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

        RegisterCapabilityHandler.register();


        // 在模組初始化時加載魔力生成速率

        // 注册 MinecraftForge 的事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 通用设置
        NetworkHandler.init(event);
        ModComponents.registerAll(); // 🛠️ 註冊所有模組零件


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



    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        // 註冊 FuelRateLoader 作為資源重載監聽器
        event.addListener(new FuelRateLoader());
        LOGGER.info("Successfully registered FuelRateLoader as a resource reload listener.");
    }
    // 客户端事件订阅器
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // 客户端设置
            ModMenuScreens.registerScreens();
            ModRenderers.registerBlockEntityRenderers();

            //   BlockEntityRenderers.register(ModBlockEntities.MANA_GENERATOR_BE.get(), ManaGeneratorRenderer::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
           // event.registerBlockEntityRenderer(ModBlockEntities.MANA_GENERATOR_BE.get(), rendererContext -> new ManaGeneratorRenderer());

        }

    }




}
