package com.github.nalamodikk.common.event;

import com.github.nalamodikk.common.command.RPGCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 📝 命令註冊處理器
 */
@EventBusSubscriber
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 註冊 RPG 命令
        RPGCommand.register(event.getDispatcher());
    }
}
