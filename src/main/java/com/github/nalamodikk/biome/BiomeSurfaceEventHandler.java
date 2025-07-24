package com.github.nalamodikk.biome;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.utils.world.BlockReplacementUtils;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * 🎯 生物群系地表事件處理器 - 超簡潔版本
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class BiomeSurfaceEventHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ChunkAccess chunk = event.getChunk();
        int totalReplaced = 0;

        // 🌱 魔力草原 - 一行搞定！
        totalReplaced += BlockReplacementUtils.replace(level, chunk, ModBiomes.MANA_PLAINS,
                BlockReplacementUtils.rules(
                        Blocks.GRASS_BLOCK, ModBlocks.MANA_GRASS_BLOCK.get(),
                        Blocks.DIRT, ModBlocks.MANA_SOIL.get()
                ),
                BlockReplacementUtils.conditions(
                        Blocks.DIRT, BlockReplacementUtils.DEEP_UNDERGROUND.and(
                                (chunk1, pos) -> ModBlocks.DEEP_MANA_SOIL.get() != null)
                )
        );

        // 🔮 水晶森林 - 也是一行！（未來使用）
        // totalReplaced += BlockReplacementUtils.replace(level, chunk, ModBiomes.CRYSTAL_FOREST,
        //     BlockReplacementUtils.rules(Blocks.STONE, ModBlocks.CRYSTAL_ORE.get()));

        // 🔥 腐化之地 - 還是一行！（未來使用）
        // totalReplaced += BlockReplacementUtils.replace(level, chunk, ModBiomes.CORRUPTED_LANDS,
        //     BlockReplacementUtils.rules(
        //         Blocks.GRASS_BLOCK, ModBlocks.CORRUPTED_GRASS.get(),
        //         Blocks.DIRT, ModBlocks.CORRUPTED_SOIL.get()
        //     ));

        // 💾 標記已修改
        if (totalReplaced > 0) {
            chunk.setUnsaved(true);
            if (KoniavacraftMod.IS_DEV) {
                KoniavacraftMod.LOGGER.debug("Surface replacement: {} blocks in chunk ({}, {})",
                        totalReplaced, chunk.getPos().x, chunk.getPos().z);
            }
        }
    }
}
