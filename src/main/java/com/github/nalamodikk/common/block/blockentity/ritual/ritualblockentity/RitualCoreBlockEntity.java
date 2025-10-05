package com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity;

import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.tracker.RitualCoreTracker;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.validator.RitualMaterialValidator;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.validator.RitualStructureValidator;
import com.github.nalamodikk.common.block.blockentity.ritual.ritualblockentity.validator.RitualValidationContext;
import com.github.nalamodikk.common.capability.ManaStorage;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import com.github.nalamodikk.register.ModBlockEntities;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * 儀式核心方塊實體 - 儀式系統的中央控制器。
 */
public class RitualCoreBlockEntity extends BlockEntity {

    private static final int MIN_RITUAL_TIME = 20; // 至少 1 秒

    private final ManaStorage manaStorage = new ManaStorage(1_000_000);
    private final RitualStructureValidator structureValidator = new RitualStructureValidator();
    private final RitualMaterialValidator materialValidator = new RitualMaterialValidator();

    private RitualState state = RitualState.IDLE;
    private int ritualProgress = 0;
    private int maxRitualTime = 0;
    private NonNullList<ItemStack> resultItems = NonNullList.withSize(9, ItemStack.EMPTY);
    private String currentRitualId = "";
    private int currentManaCost = 0;
    private RitualRecipe activeRecipe;

    public RitualCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.RITUAL_CORE_BE.get(), pos, blockState);
    }

    public void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        switch (state) {
            case IDLE -> {
                // 保持待命
            }
            case PREPARING -> handlePreparingState();
            case RUNNING -> progressRitual();
            case COMPLETED -> {
                generateRitualResults();
                setState(RitualState.IDLE);
            }
            case FAILED -> {
                cleanupFailedRitual();
                setState(RitualState.IDLE);
            }
        }
    }

    /**
     * 嘗試開始儀式：檢查結構與材料，成功後扣除催化劑。
     */
    public boolean attemptStartRitual(Player player, ItemStack catalyst) {
        if (state != RitualState.IDLE) {
            player.sendSystemMessage(Component.translatable("message.koniavacraft.ritual.already_in_progress"));
            return false;
        }
        if (level == null || level.isClientSide()) {
            return false;
        }
        if (!isValidCatalyst(catalyst)) {
            player.sendSystemMessage(Component.translatable("message.koniavacraft.ritual.invalid_catalyst"));
            return false;
        }

        RitualValidationContext context = new RitualValidationContext(level, worldPosition);

        // 驗證結構
        boolean structureValid = structureValidator.validate(context);
        if (!structureValid) {
            context.sendFirstErrorTo(player);
            // 結構驗證失敗，不消耗催化劑
            return false;
        }

        // 驗證材料
        Optional<RitualRecipe> recipeOptional = materialValidator.validate(
                context,
                manaStorage.getManaStored(),
                level.getRecipeManager()
        );

        if (recipeOptional.isEmpty()) {
            context.sendFirstErrorTo(player);
            // 材料驗證失敗，不消耗催化劑
            return false;
        }

        // 驗證通過，開始儀式
        activeRecipe = recipeOptional.get();
        currentRitualId = activeRecipe.getId().toString();
        currentManaCost = activeRecipe.getManaCost();
        maxRitualTime = Math.max(MIN_RITUAL_TIME, activeRecipe.getRitualTime());
        ritualProgress = 0;
        resetResultItems();

        // 只有在驗證全部通過後才消耗催化劑
        catalyst.shrink(1);

        setState(RitualState.PREPARING);
        return true;
    }

    /**
     * 準備階段處理：確認仍有有效配方後啟動儀式效果。
     */
    private void handlePreparingState() {
        if (activeRecipe == null) {
            setState(RitualState.FAILED);
            return;
        }
        startRitualEffects();
        setState(RitualState.RUNNING);
    }

    /**
     * 驗證催化劑是否可用。
     */
    private boolean isValidCatalyst(ItemStack stack) {
        return stack.is(ModItems.RESONANT_CRYSTAL.get()) || stack.is(ModItems.VOID_PEARL.get());
    }

    /**
     * 啟動儀式特效並確保時間參數設定妥當。
     */
    private void startRitualEffects() {
        ritualProgress = 0;
        if (maxRitualTime <= 0) {
            maxRitualTime = activeRecipe != null ? Math.max(MIN_RITUAL_TIME, activeRecipe.getRitualTime()) : 200;
        }
        // TODO: 粒子與音效
    }

    /**
     * 推進儀式進度並按配方消耗魔力。
     */
    private void progressRitual() {
        if (activeRecipe == null) {
            setState(RitualState.FAILED);
            return;
        }

        int duration = Math.max(MIN_RITUAL_TIME, maxRitualTime);
        ritualProgress++;

        boolean shouldConsume = ritualProgress % 20 == 0 || ritualProgress >= duration;
        if (shouldConsume) {
            int pulses = Math.max(1, duration / 20);
            int manaPerPulse = Math.max(1, (int) Math.ceil((double) activeRecipe.getManaCost() / pulses));
            int extracted = manaStorage.extractMana(manaPerPulse, ManaAction.EXECUTE);
            if (extracted < manaPerPulse) {
                setState(RitualState.FAILED);
                return;
            }
        }

        if (ritualProgress >= duration) {
            setState(RitualState.COMPLETED);
        }
    }

    private void resetResultItems() {
        resultItems = NonNullList.withSize(9, ItemStack.EMPTY);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null && !level.isClientSide()) {
            RitualCoreTracker.register(level, worldPosition);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            RitualCoreTracker.register(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            RitualCoreTracker.unregister(level, worldPosition);
        }
        super.setRemoved();
    }

    /**
     * 生成儀式結果：依照配方產物填入結果槽位。
     */
    private void generateRitualResults() {
        resetResultItems();
        if (activeRecipe != null) {
            var registries = level != null ? level.registryAccess() : null;
            ItemStack primary = activeRecipe.getResultItem(registries).copy();
            if (!primary.isEmpty()) {
                resultItems.set(0, primary);
            }
            var extras = activeRecipe.getAdditionalResults();
            for (int i = 0; i < extras.size() && i + 1 < resultItems.size(); i++) {
                ItemStack extra = extras.get(i);
                if (!extra.isEmpty()) {
                    resultItems.set(i + 1, extra.copy());
                }
            }
        }
        activeRecipe = null;
        setChanged();
    }

    /**
     * 清理失敗的儀式狀態。
     */
    private void cleanupFailedRitual() {
        ritualProgress = 0;
        maxRitualTime = 0;
        currentRitualId = "";
        currentManaCost = 0;
        activeRecipe = null;
        resetResultItems();
    }

    private void setState(RitualState newState) {
        this.state = newState;
        setChanged();
    }

    /**
     * 當附近基座內容變更時觸發，必要時中止儀式。
     */
    public void onPedestalContentsChanged() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (state == RitualState.RUNNING || state == RitualState.PREPARING) {
            setState(RitualState.FAILED);
            activeRecipe = null;
            resetResultItems();
            double originX = worldPosition.getX() + 0.5;
            double originY = worldPosition.getY() + 0.5;
            double originZ = worldPosition.getZ() + 0.5;
            level.players().stream()
                    .filter(player -> player.distanceToSqr(originX, originY, originZ) <= 64)
                    .forEach(player -> player.sendSystemMessage(Component.translatable("message.koniavacraft.ritual.notice.interrupted")));
        }
    }


    /**
     * 掉落暫存結果物品。
     */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : resultItems) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        resetResultItems();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("ManaStorage", manaStorage.serializeNBT(registries));
        tag.putString("State", state.name());
        tag.putInt("Progress", ritualProgress);
        tag.putInt("MaxTime", maxRitualTime);
        tag.putString("RitualId", currentRitualId);
        tag.putInt("ManaCost", currentManaCost);

        for (int i = 0; i < resultItems.size(); i++) {
            ItemStack stack = resultItems.get(i);
            if (!stack.isEmpty()) {
                tag.put("ResultItem" + i, stack.saveOptional(registries));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("ManaStorage")) {
            manaStorage.deserializeNBT(registries, tag.getCompound("ManaStorage"));
        }

        state = RitualState.valueOf(tag.getString("State"));
        ritualProgress = tag.getInt("Progress");
        maxRitualTime = tag.getInt("MaxTime");
        currentRitualId = tag.getString("RitualId");
        currentManaCost = tag.getInt("ManaCost");

        resetResultItems();
        for (int i = 0; i < resultItems.size(); i++) {
            if (tag.contains("ResultItem" + i)) {
                resultItems.set(i, ItemStack.parseOptional(registries, tag.getCompound("ResultItem" + i)));
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RitualCoreBlockEntity blockEntity) {
        blockEntity.tick();
    }

    public boolean isRitualActive() {
        return state == RitualState.RUNNING || state == RitualState.PREPARING;
    }

    /**
     * 兼容舊邏輯的啟動入口。
     */
    public void startRitual() {
        if (state == RitualState.IDLE) {
            setState(RitualState.PREPARING);
        }
    }

    public RitualState getState() {
        return state;
    }

    public int getRitualProgress() {
        return ritualProgress;
    }

    public int getMaxRitualTime() {
        return maxRitualTime;
    }

    public ManaStorage getManaStorage() {
        return manaStorage;
    }

    /**
     * 儀式狀態列舉。
     */
    public enum RitualState {
        IDLE,
        PREPARING,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
