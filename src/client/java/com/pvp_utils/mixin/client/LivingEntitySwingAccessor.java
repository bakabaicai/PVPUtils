package com.pvp_utils.mixin.client;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntitySwingAccessor {
    @Accessor("swinging")
    boolean pvp_utils$isSwinging();

    @Accessor("swinging")
    void pvp_utils$setSwinging(boolean swinging);

    @Accessor("swingTime")
    int pvp_utils$getSwingTime();

    @Accessor("swingTime")
    void pvp_utils$setSwingTime(int swingTime);

    @Accessor("swingingArm")
    void pvp_utils$setSwingingArm(InteractionHand swingingArm);

    @Invoker("getCurrentSwingDuration")
    int pvp_utils$invokeGetCurrentSwingDuration();
}
