package com.pvp_utils.client.irc.login;

import com.pvp_utils.Config;
import com.pvp_utils.client.irc.IrcBridge;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;

public final class IrcLoginWindow {
    private static final long LOGIN_TIMEOUT_MS = 30000L;
    private static final long MIN_AUTO_LOGIN_WINDOW_MS = 1200L;
    private static final long MIN_MANUAL_LOGIN_BUTTON_MS = 1500L;
    private static final long SUCCESS_WINDOW_MS = 3000L;
    private static final String SAVED_PASSWORD_PLACEHOLDER = "********";

    private IrcLoginWindow() {
    }

    public static void showBeforeGameStart() {
        if (!IrcBridge.available()) {
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

    private static JDialog createDialog(AtomicBoolean finished) {
        JDialog dialog = new JDialog((java.awt.Frame) null, "PVPUtils IRC AUTH", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);

        JPanel outer = new AntiAliasPanel(new BorderLayout(0, 0));
        JPanel root = new AntiAliasPanel(new BorderLayout(0, 14));
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 28, 24, 28));
        Font uiFont = new Font("Microsoft YaHei UI", Font.PLAIN, 15);
        Font titleFont = new Font("Microsoft YaHei UI", Font.BOLD, 28);
        Font iconFont = loadFont("/fonts/MaterialSymbolsRounded.ttf", 15f);

        JPanel header = new AntiAliasPanel(new BorderLayout(0, 6));
        JLabel title = new JLabel("PVPUtils IRC AUTH", SwingConstants.CENTER);
        title.setFont(titleFont);
        JLabel error = new JLabel(" ", SwingConstants.CENTER);
        error.setFont(uiFont.deriveFont(Font.BOLD, 13f));
        error.setForeground(new Color(190, 40, 40));
        header.add(title, BorderLayout.NORTH);
        header.add(error, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JTextField username = new JTextField(Config.ircUsername, 20);
        JPasswordField password = new JPasswordField(20);
        if (!Config.ircPasswordHash.isBlank()) {
            password.setText(SAVED_PASSWORD_PLACEHOLDER);
        }
        JCheckBox rememberPassword = new JCheckBox("记住密码", !Config.ircPasswordHash.isBlank());
        JCheckBox autoLogin = new JCheckBox("自动登录", Config.ircAutoConnect);
        JButton login = new JButton("登录");
        JButton skip = new JButton("不使用IRC");
        JLabel status = new JLabel("未注册用户会自动注册并登录。", SwingConstants.CENTER);
        status.setForeground(Color.GRAY);
        status.setFont(uiFont.deriveFont(Font.PLAIN, 14f));
        status.setPreferredSize(new Dimension(320, 28));
        JLabel connectionIcon = new JLabel("", SwingConstants.LEFT);
        JLabel connectionStatus = new JLabel("", SwingConstants.LEFT);
        JLabel protectedIcon = new JLabel("\ue32a", SwingConstants.RIGHT);
        JLabel protectedText = new JLabel("Protected by PAHP.", SwingConstants.RIGHT);
        connectionIcon.setFont(iconFont.deriveFont(Font.BOLD, 14f));
        connectionStatus.setFont(uiFont.deriveFont(Font.BOLD, 13f));
        protectedIcon.setFont(iconFont.deriveFont(Font.BOLD, 13f));
        protectedText.setFont(uiFont.deriveFont(Font.PLAIN, 12f));
        protectedIcon.setForeground(new Color(120, 120, 120));
        protectedText.setForeground(new Color(120, 120, 120));
        setConnectionStatus(connectionIcon, connectionStatus, ConnectionState.CONNECTING);
        applyFont(uiFont, username, password, rememberPassword, autoLogin, login, skip);

        JPanel form = new AntiAliasPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        JLabel usernameLabel = new JLabel("账号");
        usernameLabel.setFont(uiFont);
        form.add(usernameLabel, c);
        c.gridx = 1;
        c.gridwidth = 2;
        form.add(username, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        JLabel passwordLabel = new JLabel("密码");
        passwordLabel.setFont(uiFont);
        form.add(passwordLabel, c);
        c.gridx = 1;
        c.gridwidth = 2;
        form.add(password, c);

        JPanel loginRow = new AntiAliasPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        loginRow.add(login);
        loginRow.add(rememberPassword);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        form.add(loginRow, c);

        JPanel skipRow = new AntiAliasPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        skipRow.add(skip);
        skipRow.add(autoLogin);
        c.gridy = 3;
        form.add(skipRow, c);
        root.add(form, BorderLayout.CENTER);
        root.add(status, BorderLayout.SOUTH);
        JPanel statusBar = new AntiAliasPanel(new BorderLayout());
        statusBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JPanel connectionPanel = new AntiAliasPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        connectionPanel.add(connectionIcon);
        connectionPanel.add(connectionStatus);
        JPanel protectedPanel = new AntiAliasPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        protectedPanel.add(protectedIcon);
        protectedPanel.add(protectedText);
        statusBar.add(connectionPanel, BorderLayout.WEST);
        statusBar.add(protectedPanel, BorderLayout.EAST);
        outer.add(root, BorderLayout.CENTER);
        outer.add(statusBar, BorderLayout.SOUTH);
        dialog.setContentPane(outer);
        dialog.setPreferredSize(new Dimension(520, 380));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        startServerPing(connectionIcon, connectionStatus, finished);

        login.addActionListener(event -> {
            String user = username.getText().trim();
            String pass = new String(password.getPassword());
            boolean useSavedPassword = !Config.ircPasswordHash.isBlank() && SAVED_PASSWORD_PLACEHOLDER.equals(pass);
            if (user.isBlank() || (pass.isBlank() && !useSavedPassword)) {
                status.setForeground(new Color(180, 40, 40));
                status.setText("账号或密码不能为空。");
                return;
            }
            String hash = useSavedPassword ? Config.ircPasswordHash : sha256(pass);
            runLogin(dialog, root, form, title, error, status, connectionStatus, username, password,
                    rememberPassword, autoLogin, login, skip, finished, user, hash, false);
        });

        skip.addActionListener(event -> skipIrc(dialog, finished));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (!finished.get()) {
                    System.exit(0);
                }
            }
        });
        if (Config.ircAutoConnect && !Config.ircUsername.isBlank() && !Config.ircPasswordHash.isBlank()) {
            runLogin(dialog, root, form, title, error, status, connectionStatus, username, password,
                    rememberPassword, autoLogin, login, skip, finished, Config.ircUsername, Config.ircPasswordHash, true);
        }
        return dialog;
    }

    private static void runLogin(JDialog dialog, JPanel root, JPanel form, JLabel title, JLabel error, JLabel status,
                                 JLabel connectionStatus, JTextField usernameField, JPasswordField passwordField,
                                 JCheckBox rememberPassword, JCheckBox autoLogin, JButton login, JButton skip,
                                 AtomicBoolean finished, String username, String passwordHash, boolean automatic) {
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);
        rememberPassword.setEnabled(false);
        autoLogin.setEnabled(false);
        login.setEnabled(false);
        skip.setEnabled(false);
        error.setText(" ");
        status.setForeground(Color.GRAY);
        status.setText(automatic ? "正在自动登录..." : "正在登录...");
        new Thread(() -> {
            long startedAt = System.currentTimeMillis();
            String result = loginWithHash(username, passwordHash);
            long minimumWait = automatic ? MIN_AUTO_LOGIN_WINDOW_MS : MIN_MANUAL_LOGIN_BUTTON_MS;
            long remaining = minimumWait - (System.currentTimeMillis() - startedAt);
            if (remaining > 0L) {
                sleep(remaining);
            }
            SwingUtilities.invokeLater(() -> {
                if (result.isBlank()) {
                    Config.ircUsername = username;
                    Config.ircPasswordHash = rememberPassword.isSelected() ? passwordHash : Config.ircPasswordHash;
                    if (!rememberPassword.isSelected() && !automatic) {
                        Config.ircPasswordHash = "";
                    }
                    Config.ircAutoConnect = autoLogin.isSelected();
                    Config.ircEnabled = true;
                    Config.save();
                    showSuccess(dialog, root, form, title, error, status, connectionStatus, finished);
                } else {
                    error.setText(result);
                    status.setForeground(Color.GRAY);
                    status.setText("未注册用户会自动注册并登录。");
                    usernameField.setEnabled(true);
                    passwordField.setEnabled(true);
                    rememberPassword.setEnabled(true);
                    autoLogin.setEnabled(true);
                    login.setEnabled(true);
                    skip.setEnabled(true);
                }
            });
        }, automatic ? "PVPUtils-IRC-AutoLoginWindow" : "PVPUtils-IRC-LoginWindow").start();
    }

    private static void startServerPing(JLabel connectionIcon, JLabel connectionStatus, AtomicBoolean finished) {
        new Thread(() -> {
            while (!finished.get()) {
                SwingUtilities.invokeLater(() -> setConnectionStatus(connectionIcon, connectionStatus, ConnectionState.CONNECTING));
                boolean alive = pingServer();
                if (finished.get()) {
                    return;
                }
                SwingUtilities.invokeLater(() -> setConnectionStatus(connectionIcon, connectionStatus, alive ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED));
                if (alive) {
                    return;
                }
                sleep(5000L);
            }
        }, "PVPUtils-IRC-ServerPing").start();
    }

    private static void showSuccess(JDialog dialog, JPanel root, JPanel form, JLabel title, JLabel error, JLabel status,
                                    JLabel connectionStatus, AtomicBoolean finished) {
        UserSummary user = currentUserSummary();
        title.setText("欢迎" + blankFallback(user.username(), Config.ircUsername) + "！");
        error.setText(" ");
        root.remove(form);
        JPanel success = new AntiAliasPanel(new GridBagLayout());
        Font font = new Font("Microsoft YaHei UI", Font.PLAIN, 17);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        JLabel group = new JLabel("当前身份组：" + blankFallback(user.role(), "USER"), SwingConstants.CENTER);
        group.setFont(font);
        success.add(group, c);
        c.gridy = 1;
        JLabel rank = new JLabel("当前头衔：" + blankFallback(user.title(), "无"), SwingConstants.CENTER);
        rank.setFont(font);
        success.add(rank, c);
        root.add(success, BorderLayout.CENTER);
        status.setText("登录成功，正在启动游戏...");
        root.revalidate();
        root.repaint();
        new Thread(() -> {
            sleep(SUCCESS_WINDOW_MS);
            SwingUtilities.invokeLater(() -> {
                finished.set(true);
                dialog.dispose();
            });
        }, "PVPUtils-IRC-LoginSuccess").start();
    }

    private static void setConnectionStatus(JLabel icon, JLabel label, ConnectionState state) {
        switch (state) {
            case CONNECTED -> {
                icon.setForeground(new Color(35, 150, 60));
                icon.setText("\ue86c");
                label.setForeground(new Color(35, 150, 60));
                label.setText("已连接");
            }
            case CONNECTING -> {
                icon.setForeground(new Color(190, 145, 20));
                icon.setText("\ue5d5");
                label.setForeground(new Color(190, 145, 20));
                label.setText("连接中");
            }
            case DISCONNECTED -> {
                icon.setForeground(new Color(190, 40, 40));
                icon.setText("\ue5cd");
                label.setForeground(new Color(190, 40, 40));
                label.setText("未连接");
            }
        }
    }

    private static void applyFont(Font font, JComponent... components) {
        for (JComponent component : components) {
            component.setFont(font);
        }
    }

    private static final class AntiAliasPanel extends JPanel {
        private AntiAliasPanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (graphics instanceof Graphics2D g) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            }
            super.paintComponent(graphics);
        }
    }

    private static void skipIrc(JDialog dialog, AtomicBoolean finished) {
        if (finished.get()) {
            return;
        }
        Config.ircEnabled = false;
        Config.ircAutoConnect = false;
        Config.save();
        finished.set(true);
        dialog.dispose();
    }

    private static String loginWithHash(String username, String passwordHash) {
        try {
            Class<?> clientClass = Class.forName("com.pvp_utils.client.irc.network.PVPUtilsIrcClient");
            Object instance = clientClass.getMethod("getInstance").invoke(null);
            Method method = clientClass.getMethod("loginBlocking", String.class, String.class, long.class);
            Object result = method.invoke(instance, username, passwordHash, LOGIN_TIMEOUT_MS);
            return result == null ? "" : result.toString();
        } catch (ReflectiveOperationException e) {
            return IrcBridge.MISSING_CORE_MESSAGE;
        }
    }

    private static Font loadFont(String resourcePath, float size) {
        try (InputStream input = IrcLoginWindow.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                return new Font("Microsoft YaHei UI", Font.PLAIN, Math.round(size));
            }
            return Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(size);
        } catch (Exception ignored) {
            return new Font("Microsoft YaHei UI", Font.PLAIN, Math.round(size));
        }
    }

    private static boolean pingServer() {
        try {
            Class<?> clientClass = Class.forName("com.pvp_utils.client.irc.network.PVPUtilsIrcClient");
            Object instance = clientClass.getMethod("getInstance").invoke(null);
            Method method = clientClass.getMethod("pingBlocking", long.class);
            Object result = method.invoke(instance, 3000L);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static UserSummary currentUserSummary() {
        try {
            Class<?> userManager = Class.forName("com.pvp_utils.client.irc.user.IrcUserManager");
            Object currentUser = userManager.getMethod("currentUser").invoke(null);
            if (currentUser == null) {
                return new UserSummary("", "", "");
            }
            String username = stringMethod(currentUser, "username");
            String role = stringMethod(currentUser, "role");
            String title = stringMethod(currentUser, "title");
            return new UserSummary(username, role, title);
        } catch (ReflectiveOperationException ignored) {
            return new UserSummary("", "", "");
        }
    }

    private static String stringMethod(Object target, String name) throws ReflectiveOperationException {
        Object value = target.getClass().getMethod(name).invoke(target);
        return value == null ? "" : value.toString();
    }

    private static String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private enum ConnectionState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED
    }

    private record UserSummary(String username, String role, String title) {}
}
