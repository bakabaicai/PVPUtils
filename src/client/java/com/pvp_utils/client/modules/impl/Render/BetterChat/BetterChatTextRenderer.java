package com.pvp_utils.client.modules.impl.Render.BetterChat;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.chroma.ChromaDraw;
import com.pvp_utils.client.render.chroma.ChromaGraphics;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.Rect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ComponentCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BetterChatTextRenderer {
    private static final float FONT_SIZE = 9f;
    private static final StringSplitter SPLITTER = new StringSplitter((codePoint, style) ->
            width(new String(Character.toChars(codePoint)), style));
    private static final FormattedCharSequence INDENT = FormattedCharSequence.codepoint(' ', Style.EMPTY);

    private BetterChatTextRenderer() {}

    public static boolean usesClientFont() {
        return BetterChatNewStyle.enabled() && Config.betterChatBetterFont;
    }

    public static List<FormattedCharSequence> splitLines(FormattedText text, int maxWidth) {
        ComponentCollector collector = new ComponentCollector();
        boolean colors = Minecraft.getInstance().options.chatColors().get();
        text.visit((style, content) -> {
            collector.append(FormattedText.of(colors ? content : ChatFormatting.stripFormatting(content), style));
            return Optional.empty();
        }, Style.EMPTY);
        List<FormattedCharSequence> lines = new ArrayList<>();
        int lineWidth = Math.max(1, maxWidth - (int) Math.ceil(width(" ", Style.EMPTY)));
        SPLITTER.splitLines(collector.getResultOrEmpty(), lineWidth, Style.EMPTY, (line, continued) -> {
            FormattedCharSequence ordered = Language.getInstance().getVisualOrder(line);
            lines.add(continued ? FormattedCharSequence.composite(INDENT, ordered) : ordered);
        });
        return lines.isEmpty() ? List.of(FormattedCharSequence.EMPTY) : lines;
    }

    public static Style styleAt(GuiTextRenderState state, float mouseX, float mouseY) {
        var clip = BetterChatNewStyle.clipText(new ActiveTextCollector.Parameters(state.pose, 1f, state.scissor)).scissor();
        if (clip != null && (mouseX < clip.left() || mouseX >= clip.right()
                || mouseY < clip.top() || mouseY >= clip.bottom())) return null;
        float determinant = state.pose.m00() * state.pose.m11() - state.pose.m01() * state.pose.m10();
        if (Math.abs(determinant) < 0.000001f) return null;
        Vector2f local = new Matrix3x2f(state.pose).invert().transformPosition(mouseX, mouseY, new Vector2f());
        if (local.y < state.y || local.y >= state.y + FONT_SIZE) return null;
        float cursor = state.x;
        for (Run run : runs(state.text)) {
            if (local.x >= cursor && local.x < cursor + run.width()) return run.style();
            cursor += run.width();
        }
        return null;
    }

    private static float width(String text, Style style) {
        return style.isBold() ? ChromaDraw.boldTextWidth(text, FONT_SIZE) : ChromaDraw.textWidth(text, FONT_SIZE);
    }

    private static List<Run> runs(FormattedCharSequence sequence) {
        List<Run> runs = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        Style[] current = {Style.EMPTY};
        sequence.accept((index, style, codePoint) -> {
            if (!current[0].equals(style) && !text.isEmpty()) {
                String value = text.toString();
                runs.add(new Run(value, current[0], width(value, current[0])));
                text.setLength(0);
            }
            current[0] = style;
            text.appendCodePoint(codePoint);
            return true;
        });
        if (!text.isEmpty()) {
            String value = text.toString();
            runs.add(new Run(value, current[0], width(value, current[0])));
        }
        return runs;
    }

    public static void draw(int x, int y, ActiveTextCollector.Parameters parameters,
                            FormattedCharSequence sequence) {
        int alpha = Math.round(Math.clamp(parameters.opacity(), 0f, 1f) * 255f);
        if (alpha == 0) return;
        List<Run> runs = runs(sequence);
        Matrix3x2f pose = new Matrix3x2f(parameters.pose());
        var clip = parameters.scissor();
        ChromaGraphics.draw("better-chat-text", canvas -> {
            int save = canvas.save();
            try {
                if (clip != null) {
                    canvas.clipRect(Rect.makeLTRB(clip.left(), clip.top(), clip.right(), clip.bottom()), false);
                }
                canvas.concat(new Matrix33(pose.m00(), pose.m10(), pose.m20(),
                        pose.m01(), pose.m11(), pose.m21(), 0f, 0f, 1f));
                float cursor = x;
                for (Run run : runs) {
                    boolean bold = run.style().isBold();
                    float width = run.width();
                    int color = (alpha << 24) | (run.style().getColor() == null ? 0xFFFFFF : run.style().getColor().getValue());
                    int runSave = canvas.save();
                    try {
                        canvas.translate(cursor, y);
                        if (run.style().isItalic()) {
                            canvas.translate(2f, 0f);
                            canvas.skew(-0.2f, 0f);
                        }
                        if (bold) ChromaDraw.boldText(canvas, run.text(), 0f, 0f, FONT_SIZE, color, FONT_SIZE);
                        else ChromaDraw.text(canvas, run.text(), 0f, 0f, FONT_SIZE, color, FONT_SIZE);
                    } finally {
                        canvas.restoreToCount(runSave);
                    }
                    if (run.style().isUnderlined() || run.style().isStrikethrough()) {
                        try (Paint paint = new Paint().setColor(color)) {
                            if (run.style().isUnderlined()) canvas.drawRect(Rect.makeXYWH(cursor, y + 8f, width, 0.7f), paint);
                            if (run.style().isStrikethrough()) canvas.drawRect(Rect.makeXYWH(cursor, y + 4f, width, 0.7f), paint);
                        }
                    }
                    cursor += width;
                }
            } finally {
                canvas.restoreToCount(save);
            }
        });
    }

    private record Run(String text, Style style, float width) {}
}
