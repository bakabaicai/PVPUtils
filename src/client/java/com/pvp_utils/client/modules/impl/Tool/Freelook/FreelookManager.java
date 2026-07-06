package com.pvp_utils.client.modules.impl.Tool.Freelook;

import com.pvp_utils.Config;
import com.pvp_utils.client.KeyBindings;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FreelookManager {
    private static boolean active = false;
    private static boolean toggleActive = false;
    private static boolean wasKeyDown = false;
    private static CameraType previousCameraType = null;
    private static float yaw = 0.0f;
    private static float pitch = 0.0f;

    private FreelookManager() {
    }

    public static void tick(Minecraft client) {
        boolean canUse = Config.freelook
                && KeyBindings.freelook != null
                && client.player != null
                && client.level != null
                && client.screen == null;
        boolean keyDown = canUse && KeyBindings.freelook.isDown();

        if (!canUse) {
            toggleActive = false;
            wasKeyDown = false;
            setActive(client, false);
            return;
        }

        boolean shouldActive;
        if (Config.freelookTriggerMode == Config.FreelookTriggerMode.TOGGLE) {
            if (keyDown && !wasKeyDown) {
                toggleActive = !toggleActive;
            }
            shouldActive = toggleActive;
        } else {
            shouldActive = keyDown;
            toggleActive = false;
        }

        wasKeyDown = keyDown;
        setActive(client, shouldActive);
    }

    public static boolean isActive() {
        return active;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    public static void turn(double yawDelta, double pitchDelta) {
        if (!active) {
            return;
        }

        double sensitivity = Mth.clamp(Config.freelookSensitivity / 100.0, 0.01, 1.0);
        yaw = Mth.wrapDegrees(yaw + (float) (yawDelta * sensitivity));
        pitch = Mth.clamp(pitch + (float) (pitchDelta * sensitivity), -90.0f, 90.0f);
    }

    private static void setActive(Minecraft client, boolean value) {
        if (active == value) {
            if (active) {
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            return;
        }

        active = value;
        if (value) {
            yaw = client.player.getYRot();
            pitch = client.player.getXRot();
            previousCameraType = client.options.getCameraType();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else {
            if (previousCameraType != null) {
                client.options.setCameraType(previousCameraType);
                previousCameraType = null;
            }
        }
    }
}
