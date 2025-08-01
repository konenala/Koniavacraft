package com.github.nalamodikk.experimental.particle.item;

import com.github.nalamodikk.experimental.particle.EnergyBurstParticleSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 能量爆發測試工具
 * 右鍵使用生成能量爆發粒子效果
 */
public class EnergyBurstTestItem extends Item {

    public EnergyBurstTestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            performClientSideEffect(player);
        }
        
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @OnlyIn(Dist.CLIENT)
    private void performClientSideEffect(Player player) {
        // 在玩家前方 2 方塊處創建能量爆發效果
        Vec3 lookDirection = player.getLookAngle();
        Vec3 effectPosition = player.position().add(lookDirection.scale(2.0));
        
        // 獲取當前時間用於動畫
        float time = (float) (System.currentTimeMillis() % 10000) / 1000.0f;
        
        // 渲染能量爆發效果
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null) {
            // 這裡需要在適當的渲染事件中調用
            // 暫時先提供一個基本的實現框架
            scheduleParticleEffect(effectPosition, time);
        }
        
        // 給玩家發送消息
        player.sendSystemMessage(Component.translatable("item.koniava.energy_burst_test.used"));
    }

    @OnlyIn(Dist.CLIENT)
    private void scheduleParticleEffect(Vec3 position, float time) {
        // 實際的粒子效果需要在渲染循環中調用
        // 這裡可以存儲效果數據，然後在客戶端渲染事件中處理
        
        // 為了測試，我們可以創建一個簡單的效果管理器
        EnergyBurstEffectManager.addEffect(position, time);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.koniava.energy_burst_test.tooltip.line1"));
        tooltipComponents.add(Component.translatable("item.koniava.energy_burst_test.tooltip.line2"));
        tooltipComponents.add(Component.translatable("item.koniava.energy_burst_test.tooltip.line3"));
    }

    /**
     * 簡單的效果管理器，用於管理粒子效果的生命週期
     */
    @OnlyIn(Dist.CLIENT)
    public static class EnergyBurstEffectManager {
        private static final List<EffectData> activeEffects = new java.util.ArrayList<>();
        
        public static void addEffect(Vec3 position, float startTime) {
            activeEffects.add(new EffectData(position, startTime, 5.0f)); // 5秒持續時間
        }
        
        public static void tick() {
            float currentTime = (float) (System.currentTimeMillis() % 100000) / 1000.0f;
            activeEffects.removeIf(effect -> currentTime - effect.startTime > effect.duration);
        }
        
        public static void renderEffects() {
            // 這個方法應該在客戶端渲染事件中調用
            Minecraft mc = Minecraft.getInstance();
            if (mc.levelRenderer == null) return;
            
            float currentTime = (float) (System.currentTimeMillis() % 100000) / 1000.0f;
            
            for (EffectData effect : activeEffects) {
                float effectTime = currentTime - effect.startTime;
                if (effectTime >= 0 && effectTime <= effect.duration) {
                    // 計算效果強度（淡入淡出）
                    float intensity = 1.0f;
                    if (effectTime > effect.duration * 0.7f) {
                        intensity = 1.0f - (effectTime - effect.duration * 0.7f) / (effect.duration * 0.3f);
                    }
                    
                    // 這裡需要實際的渲染調用
                    // 由於需要 PoseStack 和 MultiBufferSource，實際渲染需要在合適的渲染事件中進行
                    
                    // 暫時在控制台輸出調試信息
                    if (effectTime < 0.1f) { // 只在開始時輸出一次
                        System.out.println("能量爆發效果開始於: " + effect.position);
                    }
                }
            }
        }
        
        private static class EffectData {
            public final Vec3 position;
            public final float startTime;
            public final float duration;
            
            public EffectData(Vec3 position, float startTime, float duration) {
                this.position = position;
                this.startTime = startTime;
                this.duration = duration;
            }
        }
    }
}