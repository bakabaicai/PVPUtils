package com.pvp_utils.client.modules.impl.Combat.combo;

public record ComboTextureAtlas(int sourceWidth, int sourceHeight, int frames, int columns) {
    public static ComboTextureAtlas of(ComboTimeline.Texture texture) {
        return new ComboTextureAtlas(texture.width, texture.height, texture.frames,
                (int) Math.ceil(Math.sqrt(texture.frames)));
    }
    public int frameHeight() { return sourceHeight / frames; }
    public int cellWidth() { return sourceWidth + 2; }
    public int cellHeight() { return frameHeight() + 2; }
    public int width() { return columns * cellWidth(); }
    public int height() { return ((frames + columns - 1) / columns) * cellHeight(); }
    public int x(int frame) { return (frame % columns) * cellWidth() + 1; }
    public int y(int frame) { return (frame / columns) * cellHeight() + 1; }
    public int sourceX(int relativeX) { return Math.floorMod(relativeX, sourceWidth); }
    public int sourceY(int frame, int relativeY) {

        return Math.floorMod(frame * frameHeight() + relativeY, sourceHeight);
    }
}
