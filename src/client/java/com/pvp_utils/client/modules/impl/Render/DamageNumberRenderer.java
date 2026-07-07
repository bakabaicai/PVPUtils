package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Matrix3x2fStack;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public final class DamageNumberRenderer {
    private static final DamageNumberRenderer INSTANCE = new DamageNumberRenderer();
    private static final long WATCH_MS = 2500L;
    private static final long NUMBER_MS = 1150L;
    private static final int DAMAGE_COLOR = 0xFFFF5555;
    private static final int HEAL_COLOR = 0xFF55FF77;

    private final Map<Integer, WatchedEntity> watched = new HashMap<>();
    private final java.util.List<FloatingNumber> numbers = new java.util.ArrayList<>();
    private final Matrix4f modelView = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private final Random random = new Random();
    private Vec3 cameraPos = Vec3.ZERO;
    private boolean projectionReady = false;

    public static DamageNumberRenderer getInstance() {
        return INSTANCE;
    }

    public void captureCamera(Matrix4fc modelViewMatrix, Matrix4fc projectionMatrix, Camera camera) {
        if (camera == null) return;
        modelView.set(modelViewMatrix);
        projection.set(projectionMatrix);
        cameraPos = camera.position();
        projectionReady = true;
    }

    public void watchAttack(Entity target) {
        if (!Config.damageNumbers || !(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        long now = System.currentTimeMillis();
        watched.put(living.getId(), new WatchedEntity(living, living.getHealth(), now + WATCH_MS));
    }

    public void syncHealth(LivingEntity entity, float newHealth) {
        if (!Config.damageNumbers || entity == null) {
            return;
        }
        WatchedEntity watchedEntity = watched.get(entity.getId());
        if (watchedEntity == null) {
            return;
        }
        long now = System.currentTimeMillis();
        float delta = newHealth - watchedEntity.lastHealth;
        if (Math.abs(delta) >= 0.05f) {
            spawn(entity, delta, now);
        }
        watchedEntity.lastHealth = newHealth;
        watchedEntity.expireAtMs = now + WATCH_MS;
    }

    public void tick(Minecraft client) {
        long now = System.currentTimeMillis();
        if (!Config.damageNumbers || client.level == null) {
            watched.clear();
            numbers.clear();
            return;
        }

        Iterator<Map.Entry<Integer, WatchedEntity>> iterator = watched.entrySet().iterator();
        while (iterator.hasNext()) {
            WatchedEntity watchedEntity = iterator.next().getValue();
            LivingEntity entity = watchedEntity.entity;
            if (entity == null || entity.isRemoved() || now > watchedEntity.expireAtMs) {
                iterator.remove();
                continue;
            }

            float current = entity.getHealth();
            float delta = current - watchedEntity.lastHealth;
            if (Math.abs(delta) >= 0.05f) {
                spawn(entity, delta, now);
                watchedEntity.lastHealth = current;
                watchedEntity.expireAtMs = now + WATCH_MS;
            }
        }

        numbers.removeIf(number -> now - number.createdAtMs > NUMBER_MS);
    }

    public void render(GuiGraphics graphics) {
        if (!Config.damageNumbers || !projectionReady || numbers.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        long now = System.currentTimeMillis();
        int guiW = client.getWindow().getGuiScaledWidth();
        int guiH = client.getWindow().getGuiScaledHeight();

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        for (FloatingNumber number : numbers) {
            float age = (now - number.createdAtMs) / (float) NUMBER_MS;
            if (age < 0f || age > 1f) continue;

            Vec3 world = number.position;
            ScreenPoint point = project(world, guiW, guiH);
            if (point == null) continue;

            float alpha = smoothstep(0.0f, 0.12f, age) * (1.0f - smoothstep(0.72f, 1.0f, age));
            float scale = 3.25f;
            int color = withAlpha(number.color, Math.round(alpha * 255f));
            String text = formatNumber(number.amount);
            int textW = client.font.width(text);
            float x = point.x - textW * scale * 0.5f;
            float y = point.y - 4f;

            pose.pushMatrix();
            pose.translate(x, y);
            pose.scale(scale, scale);
            graphics.drawString(client.font, Component.literal(text), 0, 0, color, true);
            pose.popMatrix();
        }
        pose.popMatrix();
    }

    private void spawn(LivingEntity entity, float delta, long now) {
        AABB box = entity.getBoundingBox();
        double width = Math.max(0.25, Math.max(box.getXsize(), box.getZsize()));
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = width * (0.28 + random.nextDouble() * 0.32);
        Vec3 pos = new Vec3(
                entity.getX() + Math.cos(angle) * radius,
                box.minY + box.getYsize() * (0.82 + random.nextDouble() * 0.14),
                entity.getZ() + Math.sin(angle) * radius
        );
        numbers.add(new FloatingNumber(pos, Math.abs(delta), delta < 0f ? DAMAGE_COLOR : HEAL_COLOR, now));
    }

    private ScreenPoint project(Vec3 world, int guiW, int guiH) {
        Vector4f clip = new Vector4f(
                (float) (world.x - cameraPos.x),
                (float) (world.y - cameraPos.y),
                (float) (world.z - cameraPos.z),
                1.0f
        );
        clip.mul(modelView);
        clip.mul(projection);
        if (clip.w <= 0.01f) {
            return null;
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        if (ndcX < -1.25f || ndcX > 1.25f || ndcY < -1.25f || ndcY > 1.25f) {
            return null;
        }
        return new ScreenPoint((ndcX * 0.5f + 0.5f) * guiW, (0.5f - ndcY * 0.5f) * guiH);
    }

    private String formatNumber(float value) {
        if (value >= 10f || Math.abs(value - Math.round(value)) < 0.05f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private float easeOutCubic(float value) {
        float t = 1f - clamp(value, 0f, 1f);
        return 1f - t * t * t;
    }

    private float smoothstep(float edge0, float edge1, float value) {
        float t = clamp((value - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class WatchedEntity {
        private final LivingEntity entity;
        private float lastHealth;
        private long expireAtMs;

        private WatchedEntity(LivingEntity entity, float lastHealth, long expireAtMs) {
            this.entity = entity;
            this.lastHealth = lastHealth;
            this.expireAtMs = expireAtMs;
        }
    }

    private record FloatingNumber(Vec3 position, float amount, int color, long createdAtMs) {
    }

    private record ScreenPoint(float x, float y) {
    }
}
