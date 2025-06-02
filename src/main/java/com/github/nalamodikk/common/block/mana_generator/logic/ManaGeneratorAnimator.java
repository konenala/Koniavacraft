package com.github.nalamodikk.common.block.mana_generator.logic;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.common.block.mana_generator.ManaGeneratorBlockEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.GeoAnimatable;

public class ManaGeneratorAnimator {
    private final ManaGeneratorBlockEntity entity;
    private String currentAnimation = "";
    private boolean forceRefreshAnimation = false;

    public ManaGeneratorAnimator(ManaGeneratorBlockEntity entity) {
        this.entity = entity;
    }

    public <T extends GeoAnimatable> PlayState handle(AnimationState<T> state) {
        String targetAnimation = entity.getStateManager().isWorking() ? "working" : "idle";

        if (!targetAnimation.equals(currentAnimation) || forceRefreshAnimation) {
            String oldAnimation = currentAnimation;
            state.getController().setAnimation(RawAnimation.begin().thenLoop(targetAnimation));
            currentAnimation = targetAnimation;
            forceRefreshAnimation = false;
            MagicalIndustryMod.LOGGER.debug("[Anim] Switching animation: {} → {}", oldAnimation, targetAnimation);
        }

        return PlayState.CONTINUE;
    }

    public void forceRefresh() {
        this.forceRefreshAnimation = true;
    }
}
