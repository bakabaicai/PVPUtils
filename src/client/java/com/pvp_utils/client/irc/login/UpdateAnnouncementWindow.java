package com.pvp_utils.client.irc.login;

import com.pvp_utils.client.Update;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.lang.reflect.Method;

final class UpdateAnnouncementWindow {
    private static final long DISPLAY_MS = 10000L;

    private UpdateAnnouncementWindow() {
    }

    static void showAfterLoginAsync() {
        Thread thread = new Thread(() -> {
            String announcement = fetchAnnouncement();
            if (announcement.isBlank()) {
                return;
            }
            SwingUtilities.invokeLater(() -> show(announcement));
        }, "PVPUtils-Update-Announcement");
        thread.setDaemon(true);
        thread.start();
    }

    private static String fetchAnnouncement() {
        try {
            Class<?> gateClass = Class.forName("com.pvp_utils.client.irc.network.IrcBuildGate");
            Method method = gateClass.getDeclaredMethod("fetchUpdateText");
            method.setAccessible(true);
            Object body = method.invoke(null);
            return Update.parseAnnouncement(body == null ? "" : body.toString());
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static void show(String announcement) {
        JDialog dialog = new JDialog((java.awt.Frame) null, "PVPUtils Announcement", false);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setAlwaysOnTop(true);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(new Color(248, 250, 252));
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        JLabel title = new JLabel("PVPUtils Announcement", SwingConstants.CENTER);
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 20));
        title.setForeground(new Color(24, 32, 48));
        root.add(title, BorderLayout.NORTH);

        JTextArea text = new JTextArea(announcement);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 15));
        text.setForeground(new Color(38, 48, 66));
        text.setBackground(new Color(248, 250, 252));
        text.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(430, 180));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 226, 235)));
        root.add(scroll, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        close.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(close, BorderLayout.EAST);
        root.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        Timer timer = new Timer((int) DISPLAY_MS, event -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
    }
}
