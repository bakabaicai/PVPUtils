package com.pvp_utils.client.util;

public interface ItemPhysicsRenderState {
    void pvp_utils$setItemPhysics(boolean onGround, boolean moving, boolean blockItem, int seed);

    boolean pvp_utils$itemPhysicsOnGround();

    boolean pvp_utils$itemPhysicsMoving();

    boolean pvp_utils$itemPhysicsBlockItem();

    int pvp_utils$itemPhysicsSeed();
}
