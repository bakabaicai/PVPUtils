package com.pvp_utils.client.render.skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.DirectContext;

/**
 * GPU 侧模糊背景渲染接口。
 *
 * 约定：调用方先拿到当前可用的 Skia `Canvas`，再把需要渲染的 UI 区域尺寸、圆角、
 * 模糊风格、覆盖颜色和模糊强度传进来。实现会直接从当前 framebuffer 拷贝屏幕纹理，
 * 在 GPU 上完成模糊后绘制回去，不走 CPU 读回和像素上传。
 */
public interface BlurBackgroundRenderer {
    enum BlurStyle {
        SOFT(4.5f),
        STANDARD(8.0f),
        STRONG(12.0f);

        private final float sigma;

        BlurStyle(float sigma) {
            this.sigma = sigma;
        }

        public float sigma() {
            return sigma;
        }
    }

    void render(DirectContext context, Canvas canvas, int sourceFramebufferId, float x, float y, float width, float height, float cornerRadius, BlurStyle style, int tintColor, float blurStrength);
}
