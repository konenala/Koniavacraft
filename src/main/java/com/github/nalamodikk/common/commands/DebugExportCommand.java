
package com.github.nalamodikk.common.commands;

import com.github.nalamodikk.common.block.entity.basic.MachineBlock.ModularMachineBlockEntity;
import com.github.nalamodikk.common.util.ComponentDebugger;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DebugExportCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mi")
            .then(Commands.literal("debug")
                .then(Commands.literal("export")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        BlockPos pos = player.blockPosition().below(); // 往下看（假設機器在腳下）

                        BlockEntity be = player.level().getBlockEntity(pos);

                        if (be instanceof ModularMachineBlockEntity machine) {
                            ComponentDebugger.exportToFile(machine.getGrid(), ctx.getSource().getServer(), "debug_grid.json");
                            ctx.getSource().sendSuccess(() -> Component.literal("✅ 拼裝結構已匯出至 debug_grid.json"), false);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(Component.literal("❌ 腳下不是 ModularMachineBlockEntity"));
                            return 0;
                        }
                    })
                )
            )
        );
    }
}
