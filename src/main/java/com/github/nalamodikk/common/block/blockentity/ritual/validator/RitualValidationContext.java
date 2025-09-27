package com.github.nalamodikk.common.block.blockentity.ritual.validator;

import com.github.nalamodikk.common.block.blockentity.ritual.ArcanePedestalBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ritual.ManaPylonBlockEntity;
import com.github.nalamodikk.common.block.blockentity.ritual.RuneStoneBlockEntity;
import com.github.nalamodikk.common.block.ritualblock.ChalkGlyphBlock;
import com.github.nalamodikk.common.block.ritualblock.RuneType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 儀式驗證上下文：集中保存掃描結果、錯誤訊息與統計資料。
 */
public class RitualValidationContext {

    private final Level level;
    private final BlockPos corePos;
    private final List<Component> errors = new ArrayList<>();
    private List<ArcanePedestalBlockEntity> pedestals = Collections.emptyList();
    private List<ManaPylonBlockEntity> pylons = Collections.emptyList();
    private List<RuneStoneBlockEntity> runeStones = Collections.emptyList();
    private List<ChalkGlyphInfo> chalkGlyphs = Collections.emptyList();
    private Map<String, Integer> structureSummary = Collections.emptyMap();
    private Map<RuneType, Integer> runeSummary = Collections.emptyMap();

    public RitualValidationContext(Level level, BlockPos corePos) {
        this.level = level;
        this.corePos = corePos;
    }

    public Level level() {
        return level;
    }

    public BlockPos corePos() {
        return corePos;
    }

    public void addError(Component message) {
        errors.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Optional<Component> firstError() {
        return errors.isEmpty() ? Optional.empty() : Optional.of(errors.get(0));
    }

    public void sendFirstErrorTo(Player player) {
        firstError().ifPresent(player::sendSystemMessage);
    }

    public List<Component> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void setPedestals(List<ArcanePedestalBlockEntity> pedestals) {
        this.pedestals = List.copyOf(pedestals);
    }

    public List<ArcanePedestalBlockEntity> getPedestals() {
        return pedestals;
    }

    public void setPylons(List<ManaPylonBlockEntity> pylons) {
        this.pylons = List.copyOf(pylons);
    }

    public List<ManaPylonBlockEntity> getPylons() {
        return pylons;
    }

    public void setChalkGlyphs(List<ChalkGlyphInfo> chalkGlyphs) {
        this.chalkGlyphs = List.copyOf(chalkGlyphs);
    }

    public List<ChalkGlyphInfo> getChalkGlyphs() {
        return chalkGlyphs;
    }

    public void setRuneStones(List<RuneStoneBlockEntity> runeStones) {
        this.runeStones = List.copyOf(runeStones);
    }

    public List<RuneStoneBlockEntity> getRuneStones() {
        return runeStones;
    }

    public void setStructureSummary(Map<String, Integer> summary) {
        this.structureSummary = Map.copyOf(summary);
    }

    public Map<String, Integer> getStructureSummary() {
        return structureSummary;
    }

    public void setRuneSummary(Map<RuneType, Integer> runeSummary) {
        this.runeSummary = Map.copyOf(runeSummary);
    }

    public record ChalkGlyphInfo(BlockPos pos, ChalkGlyphBlock.ChalkColor color, ChalkGlyphBlock.GlyphPattern pattern) {}

    public Map<RuneType, Integer> getRuneSummary() {
        return runeSummary;
    }

    public List<ItemStack> getPedestalOfferings() {
        List<ItemStack> offerings = new ArrayList<>();
        for (ArcanePedestalBlockEntity pedestal : pedestals) {
            ItemStack stack = pedestal.getOffering();
            if (!stack.isEmpty()) {
                offerings.add(stack.copy());
            }
        }
        return offerings;
    }
}
