package com.github.nalamodikk.experimental.particle.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * 粒子著色器註冊處理器
 * 負責註冊能量爆發粒子系統所需的著色器
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ParticleShaderHandler {

    // 著色器實例
    public static ShaderInstance energyBurstShader;
    public static ShaderInstance particleCloudShader;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        // 核心發光著色器
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "energy_core"),
                        DefaultVertexFormat.POSITION_COLOR
                ),
                shader -> energyBurstShader = shader
        );

        // 粒子雲著色器
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "particle_cloud"),
                        DefaultVertexFormat.POSITION_TEX_COLOR
                ),
                shader -> particleCloudShader = shader
        );
    }
}