package com.github.nalamodikk.common.block.normal;


import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ManaGrassBlock extends GrassBlock {

    public ManaGrassBlock(Properties properties) {
        super(properties);
    }

    // 🎨 踩踏時產生魔力粒子
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);

        if (!level.isClientSide && entity instanceof Player) {
            // 5% 機率產生粒子效果（降低觸發率）
            if (level.random.nextFloat() < 0.05f) {
                ((ServerLevel) level).sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,  // 綠色粒子，更適合草地
                        pos.getX() + 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5,
                        2, 0.2, 0.1, 0.2, 0.01  // 減少粒子數量和擴散範圍
                );

                // 輕微的魔力音效
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.BLOCKS, 0.08f, 1.8f);  // 降低音量
            }
        }
    }

    // ✨ 環境粒子效果
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        // 稍微多一點的環境粒子 (1% 機率)
        if (random.nextFloat() < 0.01f) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + random.nextDouble();

            // 使用不同的粒子類型
            level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 0.0, 0.03, 0.0);
        }
    }

    // 🌱 魔力草的特殊傳播邏輯
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 先執行原版的草地邏輯
        super.randomTick(state, level, pos, random);

        // 額外的魔力草傳播邏輯
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
            // 向周圍的魔力土壤傳播魔力草
            for (int i = 0; i < 4; ++i) {
                BlockPos targetPos = pos.offset(
                        random.nextInt(3) - 1,
                        random.nextInt(5) - 3,
                        random.nextInt(3) - 1
                );

                BlockState targetState = level.getBlockState(targetPos);

                // 如果目標是魔力土壤，且光照充足，轉換為魔力草
                if (targetState.is(ModBlocks.MANA_SOIL.get()) &&
                        level.getMaxLocalRawBrightness(targetPos.above()) >= 4 &&
                        !level.getBlockState(targetPos.above()).isCollisionShapeFullBlock(level, targetPos.above())) {

                    level.setBlockAndUpdate(targetPos, this.defaultBlockState());
                }
            }
        }
    }

    // 🌍 魔力草可以轉換回魔力土壤
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        // 如果光照不足，轉換回魔力土壤
        if (level.getMaxLocalRawBrightness(pos.above()) < 4) {
            level.setBlockAndUpdate(pos, ModBlocks.MANA_SOIL.get().defaultBlockState());
        }
    }
}