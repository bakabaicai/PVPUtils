package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Render.ArmorTransparency.ArmorTransparencyRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class ArmorTransparencyRenderStateMixin implements ArmorTransparencyRenderState {
    @Unique private boolean pvp_utils$armorTransparencyInCombat;

    @Override
    public void pvp_utils$setArmorTransparencyInCombat(boolean value) {
        pvp_utils$armorTransparencyInCombat = value;
    }

    @Override
    public boolean pvp_utils$isArmorTransparencyInCombat() {
        return pvp_utils$armorTransparencyInCombat;
    }
}
