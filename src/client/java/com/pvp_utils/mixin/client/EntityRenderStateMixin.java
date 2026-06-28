package com.pvp_utils.mixin.client;

import com.pvp_utils.client.util.NameTagPlayerFilterState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements NameTagPlayerFilterState {
    @Unique private boolean pvp_utils$nameTagRealPlayer = true;

    @Override
    public void pvp_utils$setNameTagRealPlayer(boolean realPlayer) {
        pvp_utils$nameTagRealPlayer = realPlayer;
    }

    @Override
    public boolean pvp_utils$isNameTagRealPlayer() {
        return pvp_utils$nameTagRealPlayer;
    }
}
