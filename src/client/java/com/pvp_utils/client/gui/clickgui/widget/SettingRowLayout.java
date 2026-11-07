package com.pvp_utils.client.gui.clickgui.widget;

public final class SettingRowLayout {
    public static final float MODULE_HEIGHT = 56f;
    public static final float CHILD_HEIGHT = 44f;
    public static final float LEVEL_OFFSET = 18f;

    private SettingRowLayout() {
    }

    public static float levelInset(float availableWidth, int depth) {
        return Math.min(Math.max(0, depth) * LEVEL_OFFSET, Math.max(0f, availableWidth) * 0.16f);
    }

    public static Row measure(float x, float y, float availableWidth, int depth,
                              SettingWidget widget, float leadingControlWidth) {
        float safeWidth = Math.max(0f, availableWidth);
        float inset = levelInset(safeWidth, depth);
        float rowX = x + inset;
        float width = safeWidth - inset;
        float padding = Math.min(20f, safeWidth * 0.12f);
        float headerHeight = depth == 0 ? 48f : 38f;
        float gap = depth == 0 ? 8f : 6f;
        float widgetWidth = widget == null ? 0f : Math.max(0f, widget.getWidth());
        float leading = Math.max(0f, leadingControlWidth);
        float controlSpace = Math.max(0f, width - padding * 2f - leading);
        float scale = widgetWidth > 0f ? Math.min(1f, controlSpace / widgetWidth) : 1f;
        float controlWidth = widgetWidth * scale;
        float controlHeight = widget == null ? 0f : Math.max(0f, widget.getControlHeight()) * scale;
        float widgetHeight = widget == null ? 0f : Math.max(widget.getControlHeight(), widget.getHeight()) * scale;
        boolean stacked = widget != null && width - padding * 2f - controlWidth - leading < 132f;
        float controlX = rowX + width - padding - controlWidth;
        float controlY = y + (stacked ? headerHeight + 4f : Math.max(0f, (headerHeight - controlHeight) * 0.5f));
        float bodyHeight = Math.max(headerHeight, controlY - y + widgetHeight + (stacked ? 8f : (headerHeight - controlHeight) * 0.5f));
        float textX = rowX + padding;
        float textRight = stacked || widget == null ? rowX + width - padding - leading : controlX - leading - 12f;
        return new Row(rowX, y, width, bodyHeight + gap, headerHeight, bodyHeight,
                controlX, controlY, controlWidth, controlHeight, widgetHeight, scale,
                textX, Math.max(0f, textRight - textX), stacked);
    }

    public record Row(float x, float y, float width, float height, float headerHeight, float bodyHeight,
                      float controlX, float controlY, float controlWidth, float controlHeight,
                      float widgetHeight, float controlScale, float textX, float textWidth, boolean stacked) {
        public boolean contains(float mouseX, float mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + bodyHeight;
        }

        public boolean containsControl(float mouseX, float mouseY) {
            return controlScale > 0f && mouseX >= controlX && mouseX <= controlX + controlWidth
                    && mouseY >= controlY && mouseY <= controlY + widgetHeight;
        }

        public float widgetMouseX(float mouseX) {
            return controlX + (mouseX - controlX) / Math.max(0.001f, controlScale);
        }

        public float widgetMouseY(float mouseY) {
            return controlY + (mouseY - controlY) / Math.max(0.001f, controlScale);
        }
    }
}
