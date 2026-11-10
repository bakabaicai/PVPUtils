package com.pvp_utils.client.compatibility;

import com.pvp_utils.compatibility.CompatibilityGate;
import com.pvp_utils.compatibility.CompatibilityRestart;
import com.pvp_utils.compatibility.CompatibilityWarningLayout;
import com.pvp_utils.compatibility.JavaLaunchCommand;
import com.pvp_utils.compatibility.MixinConflictDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class CompatibilityWarningScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("PVPUtils-Compatibility");
    private static final int LINE_HEIGHT = 12;
    private final Screen parent;
    private final CompatibilityGate.WarningKind kind;
    private List<MixinConflictDetector.Mod> mods = List.of();
    private int selectedMod;
    private List<Line> lines = List.of();
    private CompatibilityWarningLayout layout;
    private double scroll;
    private boolean busy;
    private boolean manualRestart;
    private boolean restartOnce;
    private CompatibilityRestart.Plan restartPlan;
    private boolean parentFailed;
    private Component status;

    public CompatibilityWarningScreen(Screen parent) {
        super(Component.translatable("pvp_utils.compatibility.title"));
        this.parent = parent;
        kind = CompatibilityGate.warning();
    }

    @Override
    protected void init() {
        mods = CompatibilityGate.conflictingMods();
        selectedMod = Math.min(selectedMod, Math.max(0, mods.size() - 1));
        List<Component> actions = new ArrayList<>();
        if (manualRestart) {
            actions.add(Component.translatable("pvp_utils.compatibility.exit"));
        } else if (restartPlan != null) {
            actions.add(Component.translatable("pvp_utils.compatibility."
                    + (restartPlan.queryCommand().isEmpty() ? "restart_execute" : "restart_execute_powershell")));
            actions.add(Component.translatable("pvp_utils.compatibility.restart_manual"));
            actions.add(Component.translatable("pvp_utils.compatibility.restart_copy"));
            actions.add(Component.translatable("pvp_utils.compatibility.restart_back"));
        } else if (kind == CompatibilityGate.WarningKind.PATCH_NOTICE) {
            actions.add(Component.translatable("pvp_utils.compatibility.confirm"));
        } else {
            actions.add(Component.translatable("pvp_utils.compatibility.disable"));
            if (kind != CompatibilityGate.WarningKind.DETECTION_FAILURE) {
                actions.add(Component.translatable("pvp_utils.compatibility."
                        + (JavaLaunchCommand.queryCommand().isEmpty() ? "restart" : "restart_powershell_option")));
            }
            if (kind == CompatibilityGate.WarningKind.MOD_CONFLICT) {
                actions.add(Component.translatable("pvp_utils.compatibility.false_positive"));
            }
        }
        int[] widths = actions.stream().mapToInt(action -> font.width(action) + 20).toArray();
        boolean selector = restartPlan == null && kind == CompatibilityGate.WarningKind.MOD_CONFLICT && mods.size() > 1;
        CompatibilityWarningLayout preliminary = CompatibilityWarningLayout.create(width, height, 0, widths, selector);
        lines = buildLines(preliminary.content().width());
        layout = CompatibilityWarningLayout.create(width, height, lines.size() * LINE_HEIGHT, widths, selector);
        scroll = Math.clamp(scroll, 0, maximumScroll());
        if (layout.selector() != null) {
            MixinConflictDetector.Mod mod = mods.get(selectedMod);
            CompatibilityWarningLayout.Rect rectangle = layout.selector();
            addRenderableWidget(Button.builder(Component.literal(mod.name() + " / " + mod.id() + " " + mod.version()), button -> {
                selectedMod = (selectedMod + 1) % mods.size();
                scroll = 0;
                rebuildWidgets();
            }).bounds(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height()).build()).active = !busy;
        }
        for (int index = 0; index < actions.size(); index++) {
            int action = index;
            CompatibilityWarningLayout.Rect rectangle = layout.actions().get(index);
            Button button = addRenderableWidget(Button.builder(actions.get(index), clicked -> handleAction(action))
                    .bounds(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height()).build());
            button.active = !busy;
            if (restartPlan == null && kind == CompatibilityGate.WarningKind.MOD_CONFLICT && index == 2 && !mods.isEmpty()) {
                button.active &= mods.get(selectedMod).id().matches("[a-z][a-z0-9_-]{1,63}");
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        if (parent != null) parent.resize(width, height);
        super.resize(width, height);
    }

    private List<Line> buildLines(int width) {
        List<Line> result = new ArrayList<>();
        if (restartPlan != null) {
            append(result, Component.translatable("pvp_utils.compatibility."
                    + (restartPlan.queryCommand().isEmpty() ? "restart_native" : "restart_powershell")), width, 0xFFDFE3EB, false);
            result.add(new Line(FormattedCharSequence.EMPTY, 0, false));
            append(result, Component.translatable("pvp_utils.compatibility."
                    + (restartOnce ? "restart_once_notice" : "restart_saved_notice")), width, 0xFFFFD078, false);
            if (!restartPlan.queryCommand().isEmpty()) {
                result.add(new Line(FormattedCharSequence.EMPTY, 0, false));
                append(result, Component.translatable("pvp_utils.compatibility.restart_query_label"), width, 0xFFDFE3EB, false);
                append(result, Component.literal(CompatibilityRestart.displayCommand(restartPlan.queryCommand())), width, 0xFF8EB7FF, false);
            }
            result.add(new Line(FormattedCharSequence.EMPTY, 0, false));
            append(result, Component.translatable("pvp_utils.compatibility.restart_helper_label"), width, 0xFFDFE3EB, false);
            append(result, Component.literal(CompatibilityRestart.displayCommand(restartPlan.helperCommand())), width, 0xFF8EB7FF, false);
            append(result, Component.translatable("pvp_utils.compatibility.restart_helper_details"), width, 0xFFAAB3C2, false);
            append(result, Component.translatable("pvp_utils.compatibility.restart_working_directory",
                    restartPlan.workingDirectory().toString()), width, 0xFFAAB3C2, false);
            appendStatus(result, width);
            return List.copyOf(result);
        }
        String message = switch (kind) {
            case MOD_CONFLICT -> "conflict_message";
            case UNSUPPORTED_PLATFORM -> "platform_message";
            case PATCH_NOTICE -> "fix_message";
            default -> "failure_message";
        };
        append(result, Component.translatable("pvp_utils.compatibility." + message), width, 0xFFDFE3EB, false);
        result.add(new Line(FormattedCharSequence.EMPTY, 0, false));
        if (kind == CompatibilityGate.WarningKind.MOD_CONFLICT) {
            append(result, Component.translatable("pvp_utils.compatibility.conflict_context"), width, 0xFFDFE3EB, false);
            result.add(new Line(FormattedCharSequence.EMPTY, 0, false));
            String names = String.join(" / ", mods.stream().map(mod -> mod.name() + " (" + mod.id() + " " + mod.version() + ")").toList());
            append(result, Component.translatable("pvp_utils.compatibility.mods", names), width, 0xFFFFD078, false);
            if (!mods.isEmpty()) {
                MixinConflictDetector.Mod selected = mods.get(selectedMod);
                if (mods.size() > 1) {
                    append(result, Component.translatable("pvp_utils.compatibility.selected", selected.name()), width, 0xFFAAB3C2, false);
                }
                for (MixinConflictDetector.Evidence evidence : CompatibilityGate.blockingConflicts()) {
                    if (!evidence.mod().id().equals(selected.id())) continue;
                    String reason = switch (evidence.kind()) {
                        case REQUIRED_EXCLUSIVE_COLLISION -> "exclusive_evidence";
                        case FINAL_EXCLUSIVE_COLLISION -> "final_evidence";
                        case OVERWRITE_REMOVES_REQUIRED_TARGET -> "overwrite_evidence";
                        case FINAL_METHOD_INJECTION -> "final_method_evidence";
                        case OVERWRITE_PRIORITY_CONFLICT -> "priority_evidence";
                    };
                    append(result, Component.translatable("pvp_utils.compatibility." + reason,
                            evidence.surviving(), evidence.required()), width, 0xFFBDC5D3, false);
                    append(result, Component.literal(evidence.targetClass().replace('/', '.') + "#" + evidence.targetMethod()),
                            width, 0xFF919BAE, false);
                }
            }
        } else if (kind == CompatibilityGate.WarningKind.UNSUPPORTED_PLATFORM || kind == CompatibilityGate.WarningKind.PATCH_NOTICE) {
            append(result, Component.translatable("pvp_utils.compatibility.environment", CompatibilityGate.platform().description()),
                    width, 0xFFFFD078, false);
            if (kind == CompatibilityGate.WarningKind.UNSUPPORTED_PLATFORM) {
                result.add(new Line(FormattedCharSequence.EMPTY, 0, false));
                append(result, Component.translatable("pvp_utils.compatibility.fix_download"), width, 0xFFDFE3EB, false);
                append(result, Component.translatable("pvp_utils.compatibility.download").withStyle(ChatFormatting.UNDERLINE),
                        width, 0xFF8EB7FF, true);
            } else {
                append(result, Component.translatable("pvp_utils.compatibility.once"), width, 0xFFAAB3C2, false);
            }
        }
        result.add(new Line(FormattedCharSequence.EMPTY, 0, false));
        append(result, Component.translatable("pvp_utils.compatibility.community"), width, 0xFFAAB3C2, false);
        if (kind != CompatibilityGate.WarningKind.PATCH_NOTICE) {
            append(result, Component.translatable("pvp_utils.compatibility.disable_scope"), width, 0xFFAAB3C2, false);
        }
        appendStatus(result, width);
        return List.copyOf(result);
    }

    private void appendStatus(List<Line> result, int width) {
        if (status != null) {
            result.add(new Line(FormattedCharSequence.EMPTY, 0, false));
            append(result, status, width, 0xFFFFD078, false);
        }
    }

    private void append(List<Line> result, Component text, int width, int color, boolean link) {
        for (FormattedCharSequence line : font.split(text, Math.max(1, width))) result.add(new Line(line, color, link));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (parent != null && !parentFailed) {
            try {
                parent.renderWithTooltipAndSubtitles(graphics, -1, -1, delta);
                return;
            } catch (RuntimeException | LinkageError exception) {
                parentFailed = true;
                LOGGER.warn("Cannot draw the menu behind the compatibility warning; using the vanilla background", exception);
            }
        }
        renderPanorama(graphics, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (layout == null) return;
        graphics.nextStratum();
        graphics.fill(0, 0, width, height, 0x99000000);
        CompatibilityWarningLayout.Rect panel = layout.panel();
        graphics.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xF51B1E26);
        Component heading = restartPlan == null ? title : Component.translatable("pvp_utils.compatibility.restart_title");
        graphics.drawString(font, heading, layout.titleX(), layout.titleY(), 0xFFFFD078, false);
        CompatibilityWarningLayout.Rect content = layout.content();
        graphics.enableScissor(content.x(), content.y(), content.right(), content.bottom());
        int first = Math.max(0, (int) scroll / LINE_HEIGHT);
        int last = Math.min(lines.size(), (int) Math.ceil((scroll + content.height()) / LINE_HEIGHT) + 1);
        for (int index = first; index < last; index++) {
            Line line = lines.get(index);
            int y = content.y() + index * LINE_HEIGHT - (int) scroll;
            boolean hovered = line.link() && content.contains(mouseX, mouseY)
                    && mouseY >= y && mouseY < y + LINE_HEIGHT && mouseX < content.x() + font.width(line.text());
            graphics.drawString(font, line.text(), content.x(), y, hovered ? 0xFFBED4FF : line.color(), false);
        }
        graphics.disableScissor();
        if (maximumScroll() > 0) {
            int barHeight = Math.max(8, (int) ((double) content.height() * content.height() / (lines.size() * LINE_HEIGHT)));
            int barY = content.y() + (int) ((content.height() - barHeight) * scroll / maximumScroll());
            graphics.fill(content.right() + 4, barY, content.right() + 6, barY + barHeight, 0xFF687184);
        }
        graphics.nextStratum();
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (layout != null && layout.content().contains(mouseX, mouseY)) {
            scroll = Math.clamp(scroll - vertical * 24, 0, maximumScroll());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!busy && layout != null && event.button() == 0 && layout.content().contains(event.x(), event.y())) {
            int index = (int) (event.y() - layout.content().y() + scroll) / LINE_HEIGHT;
            if (index >= 0 && index < lines.size() && lines.get(index).link()
                    && event.x() < layout.content().x() + font.width(lines.get(index).text())) {
                Util.getPlatform().openUri(URI.create(CompatibilityGate.FIX_URL));
                return true;
            }
        }
        super.mouseClicked(event, doubleClick);
        return true;
    }

    private void handleAction(int action) {
        if (busy) return;
        if (manualRestart) {
            minecraft.stop();
            return;
        }
        if (restartPlan != null) {
            if (action == 0) {
                restart(restartOnce, true);
            } else if (action == 1) {
                restart(restartOnce, false);
            } else if (action == 2) {
                List<String> commands = new ArrayList<>();
                if (!restartPlan.queryCommand().isEmpty()) commands.add(CompatibilityRestart.displayCommand(restartPlan.queryCommand()));
                commands.add(CompatibilityRestart.displayCommand(restartPlan.helperCommand()));
                minecraft.keyboardHandler.setClipboard(String.join(System.lineSeparator(), commands));
                status = Component.translatable("pvp_utils.compatibility.restart_copied");
                rebuildWidgets();
                scroll = maximumScroll();
            } else if (action == 3) {
                restartPlan = null;
                status = null;
                scroll = 0;
                if (CompatibilityGate.hasPendingWarning()) rebuildWidgets();
                else minecraft.setScreen(parent);
            }
            return;
        }
        try {
            if (kind == CompatibilityGate.WarningKind.PATCH_NOTICE) {
                CompatibilityGate.acknowledgePatchNotice();
                minecraft.setScreen(parent);
            } else if (action == 0) {
                CompatibilityGate.disableForCurrentLaunch();
                minecraft.setScreen(parent);
            } else if (action == 1) {
                confirmRestart(true);
            } else if (action == 2 && !mods.isEmpty()) {
                CompatibilityGate.ignoreModPermanently(mods.get(selectedMod).id());
                CompatibilityGate.WarningKind next = CompatibilityGate.warning();
                if (next == CompatibilityGate.WarningKind.MOD_CONFLICT || next == CompatibilityGate.WarningKind.UNSUPPORTED_PLATFORM) {
                    minecraft.setScreen(new CompatibilityWarningScreen(parent));
                } else {
                    confirmRestart(false);
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Cannot save compatibility preferences", exception);
            status = Component.translatable("pvp_utils.compatibility.save_failed");
            rebuildWidgets();
            scroll = maximumScroll();
        }
    }

    private void confirmRestart(boolean once) {
        restartOnce = once;
        restartPlan = CompatibilityRestart.prepare(CompatibilityGate.stateFile().getParent());
        status = null;
        scroll = 0;
        rebuildWidgets();
    }

    private void restart(boolean once, boolean automatic) {
        CompatibilityRestart.Plan plan = restartPlan;
        busy = true;
        status = Component.translatable("pvp_utils.compatibility."
                + (automatic ? "restarting" : "restart_preparing_manual"));
        rebuildWidgets();
        scroll = maximumScroll();
        Thread.ofVirtual().name("pvp-utils-compatibility-restart").start(() -> {
            if (once) {
                try {
                    CompatibilityGate.grantNextLaunch();
                } catch (IOException | RuntimeException exception) {
                    LOGGER.error("Cannot save the one-time compatibility override", exception);
                    minecraft.execute(() -> {
                        if (minecraft.screen != this) return;
                        busy = false;
                        status = Component.translatable("pvp_utils.compatibility.save_failed");
                        rebuildWidgets();
                        scroll = maximumScroll();
                    });
                    return;
                }
            }
            try {
                if (automatic) CompatibilityRestart.request(plan);
                minecraft.execute(() -> {
                    if (minecraft.screen == this) minecraft.stop();
                });
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Automatic restart is unavailable; the saved compatibility choice remains valid", exception);
                minecraft.execute(() -> {
                    if (minecraft.screen != this) return;
                    busy = false;
                    manualRestart = true;
                    status = Component.translatable("pvp_utils.compatibility.manual_restart");
                    rebuildWidgets();
                    scroll = maximumScroll();
                });
            }
        });
    }

    private double maximumScroll() {
        return layout == null ? 0 : Math.max(0, lines.size() * LINE_HEIGHT - layout.content().height());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
    }

    private record Line(FormattedCharSequence text, int color, boolean link) {
    }
}
