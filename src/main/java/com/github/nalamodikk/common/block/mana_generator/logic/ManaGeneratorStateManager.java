package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.common.MagicalIndustryMod;

public class ManaGeneratorStateManager {

    public enum Mode {
        MANA,
        ENERGY
    }

    private boolean isWorking = false;
    private Mode currentMode = Mode.MANA;

    public boolean isWorking() {
        return isWorking;
    }

    public void setWorking(boolean working) {
        this.isWorking = working;

    }

    public int getCurrentModeIndex() {
        return currentMode == Mode.MANA ? 0 : 1;
    }

    public Mode getCurrentMode() {
        return currentMode;
    }

    public boolean toggleMode(int burnTime) {
        if (isWorking || burnTime > 0) {
            MagicalIndustryMod.LOGGER.info("⚠ 無法切換模式，發電機正在運行中！");
            return false;
        }
        currentMode = (currentMode == Mode.MANA) ? Mode.ENERGY : Mode.MANA;
        return true;
    }

    public void setModeIndex(int index) {
        this.currentMode = (index == 0) ? Mode.MANA : Mode.ENERGY;
    }


    public boolean isModeMana() {
        return currentMode == Mode.MANA;
    }

    public void reset() {
        isWorking = false;
    }
}
