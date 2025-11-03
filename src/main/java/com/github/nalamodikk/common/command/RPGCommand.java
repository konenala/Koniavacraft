package com.github.nalamodikk.common.command;

import com.github.nalamodikk.common.rpg.RPGManager;
import com.github.nalamodikk.common.rpg.data.PlayerRPGData;
import com.github.nalamodikk.common.rpg.player.PlayerClass;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 🎮 RPG 系統調試命令
 *
 * 用於測試和調試 RPG 系統功能
 */
public class RPGCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("rpg")
                // /rpg info - 顯示玩家 RPG 資訊
                .then(Commands.literal("info")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        PlayerRPGData data = RPGManager.getPlayerData(player);

                        player.sendSystemMessage(Component.literal("=== RPG 資訊 ==="));
                        player.sendSystemMessage(Component.translatable("職業: %s",
                            Component.translatable(data.getPlayerClass().getTranslationKey())));
                        player.sendSystemMessage(Component.literal("等級: " + data.getLevel()));
                        player.sendSystemMessage(Component.literal("經驗: " + data.getExperience() + "/" + data.getExperienceToNextLevel()));
                        player.sendSystemMessage(Component.literal("未分配屬性點: " + data.getUnspentAttributePoints()));
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("=== 屬性 ==="));
                        player.sendSystemMessage(Component.literal("力量: " + data.getAttributes().getStrength()));
                        player.sendSystemMessage(Component.literal("智力: " + data.getAttributes().getIntelligence()));
                        player.sendSystemMessage(Component.literal("敏捷: " + data.getAttributes().getAgility()));
                        player.sendSystemMessage(Component.literal("體質: " + data.getAttributes().getVitality()));
                        player.sendSystemMessage(Component.literal("感知: " + data.getAttributes().getPerception() +
                                " (CDR: " + String.format("%.1f", data.getAttributes().getCooldownReductionPercent()) + "%)"));

                        return 1;
                    })
                )

                // /rpg setclass <warrior|mage|ranger> - 設置職業
                .then(Commands.literal("setclass")
                    .then(Commands.argument("class", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("warrior");
                            builder.suggest("mage");
                            builder.suggest("ranger");
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String className = StringArgumentType.getString(context, "class");
                            PlayerRPGData data = RPGManager.getPlayerData(player);

                            PlayerClass playerClass = PlayerClass.fromId(className);
                            data.setPlayerClass(playerClass);

                            player.sendSystemMessage(Component.translatable("✅ 職業已設置為: %s",
                                Component.translatable(playerClass.getTranslationKey())));
                            return 1;
                        })
                    )
                )

                // /rpg addexp <amount> - 添加經驗值
                .then(Commands.literal("addexp")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(context, "amount");

                            boolean leveledUp = RPGManager.giveExperience(player, amount);

                            if (leveledUp) {
                                player.sendSystemMessage(Component.literal("🎉 恭喜升級!"));
                            }
                            player.sendSystemMessage(Component.literal("✅ 獲得 " + amount + " 經驗值"));

                            return 1;
                        })
                    )
                )

                // /rpg addattr <attribute> <amount> - 分配屬性點
                .then(Commands.literal("addattr")
                    .then(Commands.argument("attribute", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("strength");
                            builder.suggest("intelligence");
                            builder.suggest("agility");
                            builder.suggest("vitality");
                            builder.suggest("perception");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                String attribute = StringArgumentType.getString(context, "attribute");
                                int amount = IntegerArgumentType.getInteger(context, "amount");

                                boolean success = RPGManager.allocateAttribute(player, attribute, amount);

                                if (success) {
                                    player.sendSystemMessage(Component.literal("✅ 成功分配 " + amount + " 點到 " + attribute));
                                } else {
                                    player.sendSystemMessage(Component.literal("❌ 分配失敗! 屬性點不足或屬性名稱錯誤"));
                                }

                                return success ? 1 : 0;
                            })
                        )
                    )
                )

                // /rpg reset - 重置所有數據 (測試用)
                .then(Commands.literal("reset")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        PlayerRPGData data = RPGManager.getPlayerData(player);

                        // 重置等級和經驗
                        data.setLevel(1);
                        data.setExperience(0);
                        data.setPlayerClass(PlayerClass.NONE);

                        // 重置屬性
                        data.resetAttributes();

                        player.sendSystemMessage(Component.literal("✅ RPG 數據已重置"));
                        return 1;
                    })
                )
        );
    }
}
