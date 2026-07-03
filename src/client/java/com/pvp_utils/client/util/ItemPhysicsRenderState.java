package com.pvp_utils.client.util;

public interface ItemPhysicsRenderState {
    void pvp_utils$setItemPhysics(boolean onGround, boolean moving, int seed);

    boolean pvp_utils$itemPhysicsOnGround();

    boolean pvp_utils$itemPhysicsMoving();

    int pvp_utils$itemPhysicsSeed();
}
