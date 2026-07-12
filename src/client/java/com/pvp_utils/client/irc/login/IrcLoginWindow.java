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
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
    private static final long REGISTER_TIMEOUT_MS = 30000L;
    private static final long MIN_AUTO_LOGIN_WINDOW_MS = 1200L;
    private static final long MIN_MANUAL_LOGIN_BUTTON_MS = 1500L;
    private static final long SUCCESS_WINDOW_MS = 3000L;
    private static final int CODE_COOLDOWN_SECONDS = 60;
    private static final String SAVED_PASSWORD_PLACEHOLDER = "********";
    private static final String LOGIN_CARD = "login";
    private static final String REGISTER_CARD = "register";

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
        JPanel root = new AntiAliasPanel(new BorderLayout(0, 16));
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 34, 28, 34));
        Font uiFont = new Font("Microsoft YaHei UI", Font.PLAIN, 15);
        Font titleFont = new Font("Microsoft YaHei UI", Font.BOLD, 28);
        Font iconFont = loadFont("/fonts/MaterialSymbolsRounded.ttf", 15f);
        Dimension fieldSize = new Dimension(330, 38);
        Dimension smallButtonSize = new Dimension(98, 34);
        Dimension wideButtonSize = new Dimension(128, 34);

        JLabel title = new JLabel("PVPUtils IRC AUTH", SwingConstants.CENTER);
        title.setFont(titleFont);
        JLabel error = new JLabel(" ", SwingConstants.CENTER);
        error.setFont(uiFont.deriveFont(Font.BOLD, 13f));
        error.setForeground(new Color(190, 40, 40));
        error.setPreferredSize(new Dimension(480, 58));

        JPanel tabs = new AntiAliasPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton loginTab = new JButton("登录");
        JButton registerTab = new JButton("注册");
        applyFont(uiFont.deriveFont(Font.BOLD, 13f), loginTab, registerTab);
        styleTabButton(loginTab, true);
        styleTabButton(registerTab, false);
        tabs.add(loginTab);
        tabs.add(registerTab);

        JPanel header = new AntiAliasPanel(new BorderLayout(0, 6));
        header.add(tabs, BorderLayout.NORTH);
        header.add(title, BorderLayout.CENTER);
        header.add(error, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JTextField loginUsername = new JTextField(Config.ircUsername, 20);
        JPasswordField loginPassword = new JPasswordField(20);
        if (!Config.ircPasswordHash.isBlank()) {
            loginPassword.setText(SAVED_PASSWORD_PLACEHOLDER);
        }
        JCheckBox rememberPassword = new JCheckBox("记住密码", !Config.ircPasswordHash.isBlank());
        JCheckBox autoLogin = new JCheckBox("自动登录", Config.ircAutoConnect);
        JButton login = new JButton("登录");
        JButton loginSkip = new JButton("不使用IRC");

        JTextField registerUsername = new JTextField(20);
        JPasswordField registerPassword = new JPasswordField(20);
        JTextField registerQq = new JTextField(20);
        JTextField registerCode = new JTextField(20);
        JButton register = new JButton("注册");
        JButton sendCode = new JButton("发送验证码");
        JButton registerSkip = new JButton("不使用IRC");
        applyFixedSize(smallButtonSize, login, register);
        applyFixedSize(wideButtonSize, loginSkip, registerSkip, sendCode);

        JLabel status = new JLabel("未注册用户请切换到注册页。", SwingConstants.CENTER);
        status.setForeground(Color.GRAY);
        status.setFont(uiFont.deriveFont(Font.PLAIN, 14f));
        status.setPreferredSize(new Dimension(380, 28));
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
        applyFont(uiFont, loginUsername, loginPassword, rememberPassword, autoLogin, login, loginSkip,
                registerUsername, registerPassword, registerQq, registerCode, register, sendCode, registerSkip);
        applyFixedSize(fieldSize, loginUsername, loginPassword, registerUsername, registerPassword, registerQq, registerCode);

        JPanel cards = new AntiAliasPanel(new CardLayout());
        cards.setPreferredSize(new Dimension(510, 350));
        cards.setMinimumSize(new Dimension(510, 350));
        JPanel loginForm = loginForm(uiFont, loginUsername, loginPassword, rememberPassword, autoLogin, login, loginSkip);
        JPanel registerForm = registerForm(uiFont, registerUsername, registerPassword, registerQq, registerCode, register, sendCode, registerSkip);
        cards.add(loginForm, LOGIN_CARD);
        cards.add(registerForm, REGISTER_CARD);
        root.add(cards, BorderLayout.CENTER);
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
        dialog.setPreferredSize(new Dimension(620, 620));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        startServerPing(connectionIcon, connectionStatus, finished);

        loginTab.addActionListener(event -> {
            ((CardLayout) cards.getLayout()).show(cards, LOGIN_CARD);
            styleTabButton(loginTab, true);
            styleTabButton(registerTab, false);
            clearMessage(error);
            status.setText("未注册用户请切换到注册页。");
        });
        registerTab.addActionListener(event -> {
            registerUsername.setText(loginUsername.getText().trim());
            ((CardLayout) cards.getLayout()).show(cards, REGISTER_CARD);
            styleTabButton(loginTab, false);
            styleTabButton(registerTab, true);
            clearMessage(error);
            status.setText(" ");
        });

        login.addActionListener(event -> {
            String user = loginUsername.getText().trim();
            String pass = new String(loginPassword.getPassword());
            boolean useSavedPassword = !Config.ircPasswordHash.isBlank() && SAVED_PASSWORD_PLACEHOLDER.equals(pass);
            if (user.isBlank() || (pass.isBlank() && !useSavedPassword)) {
                showError(error, "账号或密码不能为空。");
                return;
            }
            String hash = useSavedPassword ? Config.ircPasswordHash : sha256(pass);
            runLogin(dialog, root, cards, title, error, status, connectionStatus, rememberPassword, autoLogin,
                    login, loginSkip, finished, user, hash, false);
        });

        sendCode.addActionListener(event -> {
            String user = registerUsername.getText().trim();
            String pass = new String(registerPassword.getPassword());
            String qq = registerQq.getText().trim();
            if (user.isBlank() || pass.isBlank() || qq.isBlank()) {
                showError(error, "账号、密码和QQ号不能为空。");
                return;
            }
            if (!validQq(qq)) {
                showError(error, "QQ号格式错误。");
                return;
            }
            setRegisterButtons(false, register, sendCode, registerSkip);
            showInfo(error, "正在发送验证码...");
            new Thread(() -> {
                String result = requestRegistrationCode(user, sha256(pass), qq);
                SwingUtilities.invokeLater(() -> {
                    if (result.isBlank()) {
                        showSuccessMessage(error, "验证码已发送。请查看您的QQ邮箱和垃圾邮件分页。");
                        startCodeCooldown(sendCode, register, registerSkip);
                    } else {
                        showError(error, result);
                        setRegisterButtons(true, register, sendCode, registerSkip);
                    }
                });
            }, "PVPUtils-IRC-RegisterCode").start();
        });

        register.addActionListener(event -> {
            String user = registerUsername.getText().trim();
            String pass = new String(registerPassword.getPassword());
            String qq = registerQq.getText().trim();
            String code = registerCode.getText().trim();
            if (user.isBlank() || pass.isBlank() || qq.isBlank() || code.isBlank()) {
                showError(error, "账号、密码、QQ号和验证码不能为空。");
                return;
            }
            if (!validQq(qq)) {
                showError(error, "QQ号格式错误。");
                return;
            }
            if (!validVerificationCode(code)) {
                showError(error, "验证码格式错误。");
                return;
            }
            setRegisterButtons(false, register, sendCode, registerSkip);
            showInfo(error, "正在注册...");
            new Thread(() -> {
                String result = registerAccount(user, sha256(pass), qq, code);
                SwingUtilities.invokeLater(() -> {
                    setRegisterButtons(true, register, sendCode, registerSkip);
                    if (result.isBlank()) {
                        loginUsername.setText(user);
                        loginPassword.setText("");
                        registerPassword.setText("");
                        registerCode.setText("");
                        ((CardLayout) cards.getLayout()).show(cards, LOGIN_CARD);
                        styleTabButton(loginTab, true);
                        styleTabButton(registerTab, false);
                        showSuccessMessage(error, "注册成功，请登录。");
                    } else {
                        showError(error, result);
                    }
                });
            }, "PVPUtils-IRC-Register").start();
        });

        loginSkip.addActionListener(event -> skipIrc(dialog, finished, error));
        registerSkip.addActionListener(event -> skipIrc(dialog, finished, error));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (!finished.get()) {
                    System.exit(0);
                }
            }
        });
        if (Config.ircAutoConnect && !Config.ircUsername.isBlank() && !Config.ircPasswordHash.isBlank()) {
            runLogin(dialog, root, cards, title, error, status, connectionStatus, rememberPassword, autoLogin,
                    login, loginSkip, finished, Config.ircUsername, Config.ircPasswordHash, true);
        }
        return dialog;
    }

    private static JPanel loginForm(Font uiFont, JTextField username, JPasswordField password, JCheckBox rememberPassword,
                                    JCheckBox autoLogin, JButton login, JButton skip) {
        JPanel form = new AntiAliasPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 24, 8, 24));
        GridBagConstraints c = constraints();
        addField(form, c, uiFont, 0, "账号", username, false);
        addField(form, c, uiFont, 1, "密码", password, false);

        JPanel loginRow = rowPanel();
        loginRow.add(rememberPassword);
        loginRow.add(login);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.weightx = 1.0;
        form.add(loginRow, c);

        JPanel skipRow = rowPanel();
        skipRow.add(autoLogin);
        skipRow.add(skip);
        c.gridy = 3;
        form.add(skipRow, c);
        return form;
    }

    private static JPanel registerForm(Font uiFont, JTextField username, JPasswordField password, JTextField qq,
                                       JTextField code, JButton register, JButton sendCode, JButton skip) {
        JPanel form = new AntiAliasPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 24, 20, 24));
        GridBagConstraints c = constraints();
        addField(form, c, uiFont, 0, "账号", username, true);
        addField(form, c, uiFont, 1, "密码", password, true);
        addField(form, c, uiFont, 2, "QQ号", qq, true);
        addField(form, c, uiFont, 3, "验证码", code, true);

        JPanel actionRow = rowPanel();
        actionRow.add(sendCode);
        actionRow.add(register);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 3;
        c.weightx = 1.0;
        form.add(actionRow, c);

        JPanel skipRow = rowPanel();
        skipRow.add(skip);
        c.gridy = 5;
        form.add(skipRow, c);
        return form;
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 7, 8, 7);
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private static void addField(JPanel form, GridBagConstraints c, Font font, int row, String labelText, JComponent field, boolean wrapped) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        JLabel label = new JLabel(labelText);
        label.setFont(font);
        label.setPreferredSize(new Dimension(76, 36));
        label.setMinimumSize(new Dimension(76, 36));
        form.add(label, c);
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        form.add(wrapped ? fixedFieldPanel(field) : field, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
    }

    private static JPanel fixedFieldPanel(JComponent field) {
        JPanel panel = new AntiAliasPanel(new BorderLayout());
        Dimension size = new Dimension(300, 36);
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        panel.setMaximumSize(size);
        field.setPreferredSize(size);
        field.setMinimumSize(size);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel rowPanel() {
        JPanel panel = new AntiAliasPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        panel.setPreferredSize(new Dimension(420, 42));
        panel.setMinimumSize(new Dimension(420, 42));
        return panel;
    }

    private static void styleTabButton(JButton button, boolean selected) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(76, 30));
        button.setMinimumSize(new Dimension(76, 30));
        button.setForeground(selected ? Color.WHITE : new Color(70, 75, 85));
        button.setBackground(selected ? new Color(55, 120, 220) : new Color(238, 241, 246));
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(selected ? new Color(35, 95, 190) : new Color(205, 210, 220), 1),
                javax.swing.BorderFactory.createEmptyBorder(4, 14, 4, 14)
        ));
    }

    private static void applyFixedSize(Dimension size, JComponent... components) {
        for (JComponent component : components) {
            component.setPreferredSize(size);
            component.setMinimumSize(size);
        }
    }

    private static void runLogin(JDialog dialog, JPanel root, JPanel cards, JLabel title, JLabel error, JLabel status,
                                 JLabel connectionStatus, JCheckBox rememberPassword, JCheckBox autoLogin,
                                 JButton login, JButton skip, AtomicBoolean finished, String username,
                                 String passwordHash, boolean automatic) {
        login.setEnabled(false);
        skip.setEnabled(false);
        showInfo(error, automatic ? "正在自动登录..." : "正在登录...");
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
                    showSuccess(dialog, root, cards, title, error, status, connectionStatus, finished);
                } else {
                    showError(error, result);
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

    private static void showSuccess(JDialog dialog, JPanel root, JPanel cards, JLabel title, JLabel error, JLabel status,
                                    JLabel connectionStatus, AtomicBoolean finished) {
        UserSummary user = currentUserSummary();
        title.setText("欢迎" + blankFallback(user.username(), Config.ircUsername) + "！");
        error.setText(" ");
        root.remove(cards);
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
        showSuccessMessage(error, "登录成功，正在启动游戏...");
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

    private static void startCodeCooldown(JButton sendCode, JButton register, JButton skip) {
        register.setEnabled(true);
        skip.setEnabled(true);
        sendCode.setEnabled(false);
        final int[] remaining = {CODE_COOLDOWN_SECONDS};
        sendCode.setText("发送验证码(" + remaining[0] + "s)");
        Timer timer = new Timer(1000, null);
        timer.addActionListener(event -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                timer.stop();
                sendCode.setText("发送验证码");
                sendCode.setEnabled(true);
            } else {
                sendCode.setText("发送验证码(" + remaining[0] + "s)");
            }
        });
        timer.start();
    }

    private static void setRegisterButtons(boolean enabled, JButton register, JButton sendCode, JButton skip) {
        register.setEnabled(enabled);
        sendCode.setEnabled(enabled);
        skip.setEnabled(enabled);
    }

    private static void showError(JLabel error, String message) {
        error.setForeground(new Color(190, 40, 40));
        error.setText(formatLabelMessage(message));
    }

    private static void showSuccessMessage(JLabel label, String message) {
        label.setForeground(new Color(35, 150, 60));
        label.setText(formatLabelMessage(message));
    }

    private static void showInfo(JLabel label, String message) {
        label.setForeground(Color.GRAY);
        label.setText(formatLabelMessage(message));
    }

    private static void clearMessage(JLabel label) {
        label.setForeground(new Color(190, 40, 40));
        label.setText(" ");
    }

    private static String formatLabelMessage(String message) {
        String value = message == null ? "" : message.trim();
        if (value.isBlank()) {
            return " ";
        }
        return "<html><div style='text-align:center;'>" + escapeHtml(value).replace("\n", "<br>") + "</div></html>";
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static boolean validQq(String value) {
        return value != null && value.matches("[1-9]\\d{4,11}");
    }

    private static boolean validVerificationCode(String value) {
        return value != null && value.matches("[A-Za-z0-9]{6}");
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

    private static void skipIrc(JDialog dialog, AtomicBoolean finished, JLabel error) {
        if (finished.get()) {
            return;
        }
        String gateMessage = checkBuildGate();
        if (!gateMessage.isBlank()) {
            showError(error, gateMessage);
            return;
        }
        Config.ircEnabled = false;
        Config.ircAutoConnect = false;
        Config.save();
        finished.set(true);
        dialog.dispose();
    }

    private static String checkBuildGate() {
        try {
            Class<?> gateClass = Class.forName("com.pvp_utils.client.irc.network.IrcBuildGate");
            Method method = gateClass.getDeclaredMethod("check");
            method.setAccessible(true);
            Object result = method.invoke(null);
            return result == null ? "" : result.toString();
        } catch (ReflectiveOperationException e) {
            return IrcBridge.MISSING_CORE_MESSAGE;
        }
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

    private static String requestRegistrationCode(String username, String passwordHash, String qq) {
        try {
            Class<?> clientClass = Class.forName("com.pvp_utils.client.irc.network.PVPUtilsIrcClient");
            Object instance = clientClass.getMethod("getInstance").invoke(null);
            Method method = clientClass.getMethod("requestRegistrationCodeBlocking", String.class, String.class, String.class, long.class);
            Object result = method.invoke(instance, username, passwordHash, qq, REGISTER_TIMEOUT_MS);
            return result == null ? "" : result.toString();
        } catch (ReflectiveOperationException e) {
            return IrcBridge.MISSING_CORE_MESSAGE;
        }
    }

    private static String registerAccount(String username, String passwordHash, String qq, String code) {
        try {
            Class<?> clientClass = Class.forName("com.pvp_utils.client.irc.network.PVPUtilsIrcClient");
            Object instance = clientClass.getMethod("getInstance").invoke(null);
            Method method = clientClass.getMethod("registerBlocking", String.class, String.class, String.class, String.class, long.class);
            Object result = method.invoke(instance, username, passwordHash, qq, code, REGISTER_TIMEOUT_MS);
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
