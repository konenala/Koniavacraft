package com.github.nalamodikk.common.config;

import com.github.nalamodikk.KoniavacraftMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModCommonConfig {

    // 持有實例與規格
    public static final ModCommonConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        // 使用官方推薦的 configure 方法建立
        Pair<ModCommonConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ModCommonConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    // ===============================
    // 🎯 原有設定值
    // ===============================
    public final ModConfigSpec.IntValue manaRecipeRefreshInterval;
    public final ModConfigSpec.BooleanValue showIntroAnimation;



    private ModCommonConfig(ModConfigSpec.Builder builder) {
        // ===============================
        // 🎯 原有配置項目
        // ===============================
        manaRecipeRefreshInterval = builder
                .comment("每幾 tick 更新一次魔力合成配方結果（建議值：2～10）")
                .comment("How many ticks to refresh the mana crafting recipe result (Recommended value: 2-10)")
                .translation("koniava.config.manaRecipeRefreshInterval")
                .defineInRange("manaRecipeRefreshInterval", 2, 1, 40);

        showIntroAnimation = builder
                .comment("是否啟用登入動畫（預設開啟）")
                .comment("Enable intro animation on player login (default: true)")
                .translation("koniava.config.showIntroAnimation")
                .define("showIntroAnimation", true);

        // ===============================
        // 🌍 生物群系處理配置區段
        // ===============================


    }


    // ===============================
    // 🎧 配置事件處理器
    // ===============================

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            // 原有配置日誌
            KoniavacraftMod.LOGGER.info("載入魔力設定: manaRecipeRefreshInterval = {}",
                    INSTANCE.manaRecipeRefreshInterval.get());
            KoniavacraftMod.LOGGER.info("載入動畫設定: showIntroAnimation = {}",
                    INSTANCE.showIntroAnimation.get());


        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            // 原有配置日誌
            KoniavacraftMod.LOGGER.info("重新載入魔力設定: manaRecipeRefreshInterval = {}",
                    INSTANCE.manaRecipeRefreshInterval.get());
            KoniavacraftMod.LOGGER.info("重新載入動畫設定: showIntroAnimation = {}",
                    INSTANCE.showIntroAnimation.get());

            // 重新載入生物群系配置
            KoniavacraftMod.LOGGER.info("🔄 重新載入生物群系處理設定");
        }
    }

    // ===============================
    // 🛠️ 配置工具方法
    // ===============================

    /**
     * 📋 獲取當前配置摘要
     */


    /**
     * 🚀 一鍵性能模式設定
     */
    public static class PerformancePresets {

        public static void applyLowEndServer() {
            KoniavacraftMod.LOGGER.info("🔧 應用低配置服務器預設...");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }

        public static void applyHighEndServer() {
            KoniavacraftMod.LOGGER.info("🚀 應用高配置服務器預設...");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }

        public static void applyBalanced() {
            KoniavacraftMod.LOGGER.info("⚖️ 應用平衡設定預設...");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }
    }
}
