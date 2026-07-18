package com.pvp_utils.client.platform;

import net.fabricmc.loader.api.FabricLoader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlatformCompatibilityWarningWindow {
    private static final String PATCH_MOD_ID = "pvputils-skija-patch";
    private static final String PATCH_URL = "https://www.curseforge.com/minecraft/mc-mods/pvputils-fix";

    private PlatformCompatibilityWarningWindow() {
    }

    public static void showIfRequiredBeforeGameStart() {
        if (!shouldWarn()) {
            return;
        }

        AtomicBoolean finished = new AtomicBoolean(false);
        try {
            SwingUtilities.invokeAndWait(() -> createDialog(finished).setVisible(true));
        } catch (Exception ignored) {
            finished.set(true);
        }
        while (!finished.get()) {
            sleep(50L);
        }
    }

    public static boolean shouldWarn() {
        return !isWindows() && !FabricLoader.getInstance().isModLoaded(PATCH_MOD_ID);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static JDialog createDialog(AtomicBoolean finished) {
        JDialog dialog = new JDialog((java.awt.Frame) null, "PVPUtils Platform Warning", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);

        WarningPanel root = new WarningPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(28, 38, 28, 38));

        JLabel title = new JLabel("PVPUtils Platform Warning", SwingConstants.CENTER);
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 28));
        title.setForeground(new Color(255, 92, 64));

        JLabel subtitle = new JLabel("Unsupported Environment Detected", SwingConstants.CENTER);
        subtitle.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 15));
        subtitle.setForeground(new Color(255, 198, 91));

        JPanel header = new TransparentPanel(new BorderLayout(0, 6));
        header.add(title, BorderLayout.CENTER);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JPanel content = new TransparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextArea chinese = messageArea(
                "当前设备环境并不受 PVPUtils 完整支持，可能会出现大量未知错误。\n"
                        + "请前往下方链接下载 PVPUtils-fix 补丁 Mod，并与 PVPUtils 一起放入 mods 文件夹加载。",
                15f,
                new Color(255, 235, 220)
        );
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 16, 0);
        content.add(chinese, gbc);

        JTextArea english = messageArea(
                "Your current device environment is not fully supported by PVPUtils and may cause unknown issues.\n"
                        + "Please download the PVPUtils-fix patch mod from the link below and load it together with PVPUtils in the mods folder.",
                13f,
                new Color(206, 218, 232)
        );
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 18, 0);
        content.add(english, gbc);

        JLabel link = new JLabel(PATCH_URL, SwingConstants.CENTER);
        link.setFont(new Font("Consolas", Font.BOLD, 13));
        link.setForeground(new Color(92, 184, 255));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                openPatchPage();
            }
        });
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        content.add(link, gbc);
        root.add(content, BorderLayout.CENTER);

        JButton open = button("Download Fix", new Color(255, 100, 64), Color.WHITE);
        open.addActionListener(event -> openPatchPage());
        JButton continueAnyway = button("Continue", new Color(45, 53, 68), new Color(218, 226, 236));
        continueAnyway.addActionListener(event -> dialog.dispose());

        JPanel actions = new TransparentPanel(new GridBagLayout());
        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.gridy = 0;
        buttonGbc.insets = new Insets(0, 8, 0, 8);
        actions.add(open, buttonGbc);
        actions.add(continueAnyway, buttonGbc);
        root.add(actions, BorderLayout.SOUTH);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                finished.set(true);
            }
        });

        dialog.setContentPane(root);
        dialog.setSize(new Dimension(760, 500));
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    private static JTextArea messageArea(String text, float size, Color color) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Microsoft YaHei UI", Font.BOLD, (int) size));
        area.setForeground(color);
        area.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        area.setPreferredSize(new Dimension(640, size > 14f ? 72 : 76));
        return area;
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(180, 42));
        button.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static void openPatchPage() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(PATCH_URL));
            }
        } catch (Exception ignored) {
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static class TransparentPanel extends JPanel {
        TransparentPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }
    }

    private static final class WarningPanel extends TransparentPanel {
        WarningPanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(10, 13, 20));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(118, 24, 20, 210));
            g.fillOval(-120, -120, 360, 360);
            g.setColor(new Color(230, 70, 42, 115));
            g.fillOval(getWidth() - 260, 40, 320, 320);
            g.setColor(new Color(255, 126, 54, 90));
            g.fillRoundRect(24, 22, getWidth() - 48, getHeight() - 44, 34, 34);
            g.setColor(new Color(17, 22, 34, 236));
            g.fillRoundRect(28, 26, getWidth() - 56, getHeight() - 52, 30, 30);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
