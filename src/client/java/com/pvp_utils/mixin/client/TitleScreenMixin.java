package com.pvp_utils.mixin.client;

import com.pvp_utils.client.Version;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void pvp_utils$renderVersionText(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        String version = Version.displayName();
        String type = Version.typeName();
        int x = 4;
        int y = 4;
        if (type.isEmpty()) {
            guiGraphics.drawString(client.font, version, x, y, 0xFFFFFFFF, false);
            return;
        }

        if (Version.DEBUG) {
            guiGraphics.drawString(client.font, "DEBUG", x, y + client.font.lineHeight + 2, 0xFFFFD34D, false);
        }

        String marker = "-" + type;
        int typeStart = version.indexOf(marker);
        if (typeStart < 0) {
            guiGraphics.drawString(client.font, version, x, y, 0xFFFFFFFF, false);
            return;
        }

        typeStart += 1;
        int typeEnd = typeStart + type.length();
        String before = version.substring(0, typeStart);
        String typed = version.substring(typeStart, typeEnd);
        String after = version.substring(typeEnd);
        int typeColor = Version.TYPE == 1 ? 0xFFFF4444 : 0xFFFFD34D;
        guiGraphics.drawString(client.font, before, x, y, 0xFFFFFFFF, false);
        int tx = x + client.font.width(before);
        guiGraphics.drawString(client.font, typed, tx, y, typeColor, false);
        guiGraphics.drawString(client.font, after, tx + client.font.width(typed), y, 0xFFFFFFFF, false);
    }
}
