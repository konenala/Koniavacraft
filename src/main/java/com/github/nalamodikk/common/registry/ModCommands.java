package com.github.nalamodikk.common.registry;


import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.commands.DebugExportCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MagicalIndustryMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // ✅ 註冊所有模組指令
        DebugExportCommand.register(dispatcher);
        // 將來這邊可以加更多，如：ComponentQueryCommand.register(dispatcher);
    }
}
