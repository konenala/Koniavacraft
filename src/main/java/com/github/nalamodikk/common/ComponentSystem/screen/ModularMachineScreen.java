package com.github.nalamodikk.common.ComponentSystem.screen;

import com.github.nalamodikk.client.screenAPI.DynamicTooltip;
import com.github.nalamodikk.client.screenAPI.GenericButtonWithTooltip;
import com.github.nalamodikk.common.ComponentSystem.API.machine.component.AutoCrafterComponent;
import com.github.nalamodikk.common.ComponentSystem.block.blockentity.MachineBlock.ModularMachineBlockEntity;
import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.ComponentSystem.network.ToggleAutoCraftingMessage;
import com.github.nalamodikk.common.register.handler.RegisterNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ModularMachineScreen extends AbstractContainerScreen<ModularMachineMenu> {
    private DynamicTooltip tooltip;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/modular_machine_gui.png");

    private GenericButtonWithTooltip toggleButton;
    private boolean cachedToggle = false; // 畫面顯示用快取

    public ModularMachineScreen(ModularMachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        // 🔘 建立一顆按鈕用來控制 AutoCrafterComponent 狀態
        this.tooltip = new DynamicTooltip(() -> {
            boolean enabled = cachedToggle;
            return List.of(Component.translatable(
                    enabled ? "button.magical_industry.auto_crafting_enabled"
                            : "button.magical_industry.auto_crafting_disabled"
            ));
        });

        this.toggleButton = new GenericButtonWithTooltip(
                leftPos + 130, topPos + 20, 36, 20,
                Component.translatable("button.magical_industry.toggle_auto_crafting"),
                new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/button_toggle.png"),
                36, 20,
                b -> RegisterNetworkHandler.NETWORK_CHANNEL.sendToServer(
                        new ToggleAutoCraftingMessage(menu.getMachinePos())
                ),
                tooltip::toComponentList
        );

    }

    private int tickCounter = 0;

    @Override
    protected void containerTick() {
        super.containerTick();
        tickCounter++;
        if (tickCounter % 10 == 0) { // 每 10 tick（0.5 秒）更新一次
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(menu.getMachinePos());
            if (be instanceof ModularMachineBlockEntity machine) {
                machine.getComponentGrid()
                        .findFirstComponent(AutoCrafterComponent.class)
                        .ifPresent(comp -> {
                            this.cachedToggle = comp.isGuiToggle();
                        });

            }
        }

    }


    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title, 8, 6, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}

