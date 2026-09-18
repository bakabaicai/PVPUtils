package com.pvp_utils.mixin.client;

import com.pvp_utils.client.util.ItemPhysicsRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemPhysicsRenderState {
    @Unique private boolean pvp_utils$itemPhysicsOnGround;
    @Unique private boolean pvp_utils$itemPhysicsMoving;
    @Unique private boolean pvp_utils$itemPhysicsBlockItem;
    @Unique private int pvp_utils$itemPhysicsSeed;

    @Override
    public void pvp_utils$setItemPhysics(boolean onGround, boolean moving, boolean blockItem, int seed) {
        this.pvp_utils$itemPhysicsOnGround = onGround;
        this.pvp_utils$itemPhysicsMoving = moving;
        this.pvp_utils$itemPhysicsBlockItem = blockItem;
        this.pvp_utils$itemPhysicsSeed = seed;
    }

    @Override
    public boolean pvp_utils$itemPhysicsOnGround() {
        return pvp_utils$itemPhysicsOnGround;
    }

    @Override
    public boolean pvp_utils$itemPhysicsMoving() {
        return pvp_utils$itemPhysicsMoving;
    }

    @Override
    public boolean pvp_utils$itemPhysicsBlockItem() {
        return pvp_utils$itemPhysicsBlockItem;
    }

    @Override
    public int pvp_utils$itemPhysicsSeed() {
        return pvp_utils$itemPhysicsSeed;
    }
}
