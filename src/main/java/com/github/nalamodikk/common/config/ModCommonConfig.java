package com.github.nalamodikk.common.config;

import com.github.nalamodikk.KoniavacraftMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
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

    // 設定值本體
    public final ModConfigSpec.IntValue manaRecipeRefreshInterval;
    public final ModConfigSpec.BooleanValue showIntroAnimation;

    private ModCommonConfig(ModConfigSpec.Builder builder) {
        manaRecipeRefreshInterval = builder
                .comment("每幾 tick 更新一次魔力合成配方結果（建議值：2～10）")
                .comment("How many ticks to refresh the mana crafting recipe result (Recommended value: 2-10)")
                .translation("koniava.config.manaRecipeRefreshInterval")
                .defineInRange("manaRecipeRefreshInterval", 2, 1, 40);

        // ✅ 加入登入動畫設定
        showIntroAnimation = builder
                .comment("是否啟用登入動畫（預設開啟）")
                .comment("Enable intro animation on player login (default: true)")
                .translation("koniava.config.showIntroAnimation")
                .define("showIntroAnimation", true);
    }



    // 讀取事件處理器
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            KoniavacraftMod.LOGGER.info("載入魔力設定: manaRecipeRefreshInterval = {}", INSTANCE.manaRecipeRefreshInterval.get());
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            KoniavacraftMod.LOGGER.info("重新載入魔力設定: manaRecipeRefreshInterval = {}", INSTANCE.manaRecipeRefreshInterval.get());
        }
    }

}
