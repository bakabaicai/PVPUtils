package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DroppedItemRadarRenderer extends SkiaTextHudRenderer {
    private static final DroppedItemRadarRenderer INSTANCE = new DroppedItemRadarRenderer();
    private static final double RADIUS = 64.0;
    private static final int MAX_LINES = 8;

    private DroppedItemRadarRenderer() {
    }

    public static DroppedItemRadarRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean enabled() {
        return Config.droppedItemRadar;
    }

    @Override
    protected Config.HudStyle style() {
        return Config.droppedItemRadarStyle;
    }

    @Override
    protected boolean backgroundEnabled() {
        return Config.droppedItemRadarBackground;
    }

    @Override
    protected float configX() {
        return Config.droppedItemRadarX;
    }

    @Override
    protected void setConfigX(float value) {
        Config.droppedItemRadarX = value;
    }

    @Override
    protected float configY() {
        return Config.droppedItemRadarY;
    }

    @Override
    protected void setConfigY(float value) {
        Config.droppedItemRadarY = value;
    }

    @Override
    protected float configScale() {
        return Config.droppedItemRadarScale;
    }

    @Override
    protected void setConfigScale(float value) {
        Config.droppedItemRadarScale = value;
    }

    @Override
    protected List<Line> lines() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return List.of();
        }
        Vec3 pos = client.player.position();
        AABB box = new AABB(pos.x - RADIUS, pos.y - RADIUS, pos.z - RADIUS, pos.x + RADIUS, pos.y + RADIUS, pos.z + RADIUS);
        List<ItemEntity> items = client.level.getEntitiesOfClass(ItemEntity.class, box);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemEntity item : items) {
            String name = item.getItem().getHoverName().getString();
            counts.merge(name, item.getItem().getCount(), Integer::sum);
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < sorted.size() && i < MAX_LINES; i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            lines.add(new Line(entry.getKey() + " x" + entry.getValue(), 0xFFF2F4F8));
        }
        return lines;
    }
}
