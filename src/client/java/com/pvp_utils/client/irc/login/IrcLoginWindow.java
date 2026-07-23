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
import java.awt.AlphaComposite;
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
    private static final long ACTIVATE_TIMEOUT_MS = 30000L;
    private static final long MIN_AUTO_LOGIN_WINDOW_MS = 1200L;
    private static final long MIN_MANUAL_LOGIN_BUTTON_MS = 1500L;
    private static final long SUCCESS_WINDOW_MS = 3000L;
    private static final int CODE_COOLDOWN_SECONDS = 60;
    private static final int WINDOW_WIDTH = 620;
    private static final int WINDOW_HEIGHT = 620;
    private static final String SAVED_PASSWORD_PLACEHOLDER = "********";
    private static final String LOGIN_CARD = "login";
    private static final String REGISTER_CARD = "register";
    private static final String ACTIVATE_CARD = "activate";

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

        AlphaPanel outer = new AlphaPanel(new BorderLayout(0, 0));
        outer.setAlpha(0f);
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

        SlidingTabPanel tabs = new SlidingTabPanel(3);
        JButton loginTab = new JButton("登录");
        JButton registerTab = new JButton("注册");
        JButton activateTab = new JButton("激活");
        applyFont(uiFont.deriveFont(Font.BOLD, 13f), loginTab, registerTab, activateTab);
        styleTabButton(loginTab, true);
        styleTabButton(registerTab, false);
        styleTabButton(activateTab, false);
        tabs.addTab(loginTab);
        tabs.addTab(registerTab);
        tabs.addTab(activateTab);

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
        JTextField activationUsername = new JTextField(Config.ircUsername, 20);
        JTextField activationKey = new JTextField(20);
        JButton activate = new JButton("激活");
        applyFixedSize(smallButtonSize, login, register, activate);
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
                registerUsername, registerPassword, registerQq, registerCode, register, sendCode, registerSkip,
                activationUsername, activationKey, activate);
        applyFixedSize(fieldSize, loginUsername, loginPassword, registerUsername, registerPassword, registerQq, registerCode, activationUsername, activationKey);

        AlphaPanel cards = new AlphaPanel(new CardLayout());
        cards.setPreferredSize(new Dimension(510, 350));
        cards.setMinimumSize(new Dimension(510, 350));
        JPanel loginForm = loginForm(uiFont, loginUsername, loginPassword, rememberPassword, autoLogin, login, loginSkip);
        JPanel registerForm = registerForm(uiFont, registerUsername, registerPassword, registerQq, registerCode, register, sendCode, registerSkip);
        JPanel activateForm = activateForm(uiFont, activationUsername, activationKey, activate);
        cards.add(loginForm, LOGIN_CARD);
        cards.add(registerForm, REGISTER_CARD);
        cards.add(activateForm, ACTIVATE_CARD);
        AlphaPanel statusFade = new AlphaPanel(new BorderLayout());
        statusFade.add(status, BorderLayout.CENTER);
        root.add(cards, BorderLayout.CENTER);
        root.add(statusFade, BorderLayout.SOUTH);

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
        dialog.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        playOpenAnimation(dialog, outer, WINDOW_WIDTH, WINDOW_HEIGHT);
        startServerPing(connectionIcon, connectionStatus, finished);

        loginTab.addActionListener(event -> {
            switchLoginPage(cards, statusFade, () -> {
                ((CardLayout) cards.getLayout()).show(cards, LOGIN_CARD);
                tabs.select(0);
                styleTabButton(loginTab, true);
                styleTabButton(registerTab, false);
                styleTabButton(activateTab, false);
                clearMessage(error);
                status.setText("未注册用户请切换到注册页。");
            });
        });
        registerTab.addActionListener(event -> {
            switchLoginPage(cards, statusFade, () -> {
                registerUsername.setText(loginUsername.getText().trim());
                ((CardLayout) cards.getLayout()).show(cards, REGISTER_CARD);
                tabs.select(1);
                styleTabButton(loginTab, false);
                styleTabButton(registerTab, true);
                styleTabButton(activateTab, false);
                clearMessage(error);
                status.setText(" ");
            });
        });
        activateTab.addActionListener(event -> {
            switchLoginPage(cards, statusFade, () -> {
                ((CardLayout) cards.getLayout()).show(cards, ACTIVATE_CARD);
                tabs.select(2);
                styleTabButton(loginTab, false);
                styleTabButton(registerTab, false);
                styleTabButton(activateTab, true);
                clearMessage(error);
                status.setText("请输入要激活的 IRC 账号和卡密。");
            });
        });

        activate.addActionListener(event -> {
            String username = activationUsername.getText().trim();
            String key = activationKey.getText().trim();
            if (username.isBlank()) {
                showError(error, "Account cannot be empty.");
                return;
            }
            if (key.isBlank()) {
                showError(error, "Activation key cannot be empty.");
                return;
            }
            activate.setEnabled(false);
            showInfo(error, "Activating...");
            new Thread(() -> {
                String result = redeemKey(username, key);
                SwingUtilities.invokeLater(() -> {
                    activate.setEnabled(true);
                    if (result.startsWith("Redeemed successfully!")) {
                        activationKey.setText("");
                        showSuccessMessage(error, result);
                    } else {
                        showError(error, result);
                    }
                });
            }, "PVPUtils-IRC-Activation").start();
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
            runLogin(dialog, outer, root, cards, title, error, status, connectionStatus, rememberPassword, autoLogin,
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
                        tabs.select(0);
                        styleTabButton(loginTab, true);
                        styleTabButton(registerTab, false);
                        styleTabButton(activateTab, false);
                        showSuccessMessage(error, "注册成功，请登录。");
                    } else {
                        showError(error, result);
                    }
                });
            }, "PVPUtils-IRC-Register").start();
        });

        loginSkip.addActionListener(event -> skipIrc(dialog, outer, finished, error));
        registerSkip.addActionListener(event -> skipIrc(dialog, outer, finished, error));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (!finished.get()) {
                    System.exit(0);
                }
            }
        });
        if (Config.ircAutoConnect && !Config.ircUsername.isBlank() && !Config.ircPasswordHash.isBlank()) {
            runLogin(dialog, outer, root, cards, title, error, status, connectionStatus, rememberPassword, autoLogin,
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

    private static JPanel activateForm(Font uiFont, JTextField username, JTextField activationKey, JButton activate) {
        JPanel form = new AntiAliasPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(50, 24, 50, 24));
        GridBagConstraints c = constraints();
        addField(form, c, uiFont, 0, "账号", username, false);
        addField(form, c, uiFont, 1, "卡密", activationKey, false);
        JPanel actionRow = rowPanel();
        actionRow.add(activate);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.weightx = 1.0;
        form.add(actionRow, c);
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
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(76, 30));
        button.setMinimumSize(new Dimension(76, 30));
        button.setForeground(selected ? Color.WHITE : new Color(70, 75, 85));
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(205, 210, 220, selected ? 0 : 255), 1),
                javax.swing.BorderFactory.createEmptyBorder(4, 14, 4, 14)
        ));
    }

    private static void applyFixedSize(Dimension size, JComponent... components) {
        for (JComponent component : components) {
            component.setPreferredSize(size);
            component.setMinimumSize(size);
        }
    }

    private static void playOpenAnimation(JDialog dialog, AlphaPanel content, int targetW, int targetH) {
        int startW = 80;
        int startH = 80;
        int overshootW = Math.round(targetW * 1.045f);
        int overshootH = Math.round(targetH * 1.045f);
        int centerX = dialog.getX() + targetW / 2;
        int centerY = dialog.getY() + targetH / 2;
        dialog.setBounds(centerX - startW / 2, centerY - startH / 2, startW, startH);

        long startMs = System.currentTimeMillis();
        int growDuration = 260;
        int settleDuration = 140;
        Timer timer = new Timer(16, null);
        timer.addActionListener(event -> {
            long elapsed = System.currentTimeMillis() - startMs;
            int width;
            int height;
            if (elapsed <= growDuration) {
                float t = easeOutCubic(elapsed / (float) growDuration);
                width = lerp(startW, overshootW, t);
                height = lerp(startH, overshootH, t);
            } else {
                float t = easeInCubic((elapsed - growDuration) / (float) settleDuration);
                width = lerp(overshootW, targetW, t);
                height = lerp(overshootH, targetH, t);
            }
            dialog.setBounds(centerX - width / 2, centerY - height / 2, width, height);
            dialog.revalidate();
            if (elapsed >= growDuration + settleDuration) {
                timer.stop();
                dialog.setBounds(centerX - targetW / 2, centerY - targetH / 2, targetW, targetH);
                dialog.revalidate();
                fadeContent(content, 0f, 1f, 220, null);
            }
        });
        timer.start();
    }

    private static void playCloseAnimation(JDialog dialog, AlphaPanel content, Runnable onComplete) {
        fadeContent(content, content.getAlpha(), 0f, 180, () -> {
            int startW = dialog.getWidth();
            int startH = dialog.getHeight();
            int overshootW = Math.round(WINDOW_WIDTH * 1.045f);
            int overshootH = Math.round(WINDOW_HEIGHT * 1.045f);
            int targetW = 80;
            int targetH = 80;
            int centerX = dialog.getX() + startW / 2;
            int centerY = dialog.getY() + startH / 2;
            long startMs = System.currentTimeMillis();
            int expandDuration = 140;
            int shrinkDuration = 260;
            Timer timer = new Timer(16, null);
            timer.addActionListener(event -> {
                long elapsed = System.currentTimeMillis() - startMs;
                int width;
                int height;
                if (elapsed <= expandDuration) {
                    float t = easeOutCubic(elapsed / (float) expandDuration);
                    width = lerp(startW, overshootW, t);
                    height = lerp(startH, overshootH, t);
                } else {
                    float t = easeInCubic((elapsed - expandDuration) / (float) shrinkDuration);
                    width = lerp(overshootW, targetW, t);
                    height = lerp(overshootH, targetH, t);
                }
                dialog.setBounds(centerX - width / 2, centerY - height / 2, width, height);
                dialog.revalidate();
                if (elapsed >= expandDuration + shrinkDuration) {
                    timer.stop();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            timer.start();
        });
    }

    private static void fadeContent(AlphaPanel content, float from, float to, int durationMs, Runnable onComplete) {
        long startMs = System.currentTimeMillis();
        Timer timer = new Timer(16, null);
        timer.addActionListener(event -> {
            float t = easeOutCubic((System.currentTimeMillis() - startMs) / (float) durationMs);
            content.setAlpha(from + (to - from) * t);
            if (t >= 1f) {
                timer.stop();
                content.setAlpha(to);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        timer.start();
    }

    private static void switchLoginPage(AlphaPanel cards, AlphaPanel status, Runnable switcher) {
        fadeContent(cards, cards.getAlpha(), 0f, 120, () -> {
            fadeContent(status, status.getAlpha(), 0f, 80, () -> {
                if (switcher != null) {
                    switcher.run();
                }
                cards.revalidate();
                cards.repaint();
                status.revalidate();
                status.repaint();
                fadeContent(status, 0f, 1f, 120, null);
                fadeContent(cards, 0f, 1f, 160, null);
            });
        });
    }

    private static int lerp(int start, int end, float t) {
        return Math.round(start + (end - start) * t);
    }

    private static float easeOutCubic(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private static float easeInCubic(float value) {
        float t = Math.max(0f, Math.min(1f, value));
        return t * t * t;
    }

    private static void runLogin(JDialog dialog, AlphaPanel outer, JPanel root, JPanel cards, JLabel title, JLabel error, JLabel status,
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
                    showSuccess(dialog, outer, root, cards, title, error, status, connectionStatus, finished);
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
                sleep(5000L);
            }
        }, "PVPUtils-IRC-ServerPing").start();
    }

    private static void showSuccess(JDialog dialog, AlphaPanel outer, JPanel root, JPanel cards, JLabel title, JLabel error, JLabel status,
                                    JLabel connectionStatus, AtomicBoolean finished) {
        UserSummary user = currentUserSummary();
        title.setText("欢迎" + blankFallback(user.username(), Config.ircUsername) + "！");
        error.setText(" ");
        status.setText(" ");
        status.setVisible(false);
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
        c.gridy = 2;
        JLabel services = new JLabel(formatServices(user.services()), SwingConstants.CENTER);
        services.setFont(font);
        success.add(services, c);
        root.add(success, BorderLayout.CENTER);
        Timer refresh = new Timer(250, event -> {
            UserSummary latest = currentUserSummary();
            title.setText("欢迎" + blankFallback(latest.username(), Config.ircUsername) + "！");
            group.setText("当前身份组：" + blankFallback(latest.role(), "USER"));
            rank.setText("当前头衔：" + blankFallback(latest.title(), "无"));
            services.setText(formatServices(latest.services()));
        });
        refresh.start();
        showSuccessMessage(error, "登录成功，正在启动游戏...");
        root.revalidate();
        root.repaint();
        new Thread(() -> {
            sleep(SUCCESS_WINDOW_MS);
            SwingUtilities.invokeLater(() -> {
                refresh.stop();
                playCloseAnimation(dialog, outer, () -> {
                    finished.set(true);
                    dialog.dispose();
                    UpdateAnnouncementWindow.showAfterLoginAsync();
                });
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
        shakeWindow(error);
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

    private static void shakeWindow(JComponent component) {
        java.awt.Window window = SwingUtilities.getWindowAncestor(component);
        if (window == null || !window.isShowing()) {
            return;
        }
        int originX = window.getX();
        int originY = window.getY();
        int[] offsets = {0, -10, 10, -8, 8, -5, 5, -2, 2, 0};
        final int[] frame = {0};
        Timer timer = new Timer(18, null);
        timer.addActionListener(event -> {
            if (frame[0] >= offsets.length) {
                timer.stop();
                window.setLocation(originX, originY);
                return;
            }
            window.setLocation(originX + offsets[frame[0]], originY);
            frame[0]++;
        });
        timer.start();
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

    private static String formatServices(String services) {
        String value = blankFallback(services, "无");
        return "<html><div style='text-align:center;'>激活的服务：" + escapeHtml(value).replace("\n", "<br>") + "</div></html>";
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

    private static class AntiAliasPanel extends JPanel {
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

    private static void skipIrc(JDialog dialog, AlphaPanel outer, AtomicBoolean finished, JLabel error) {
        if (finished.get()) {
            return;
        }
        String gateMessage = checkBuildExpiryOnly();
        if (!gateMessage.isBlank()) {
            showError(error, gateMessage);
            return;
        }
        String deviceBanMessage = checkDeviceBan();
        if (!deviceBanMessage.isBlank()) {
            showError(error, deviceBanMessage);
            return;
        }
        Config.clearIrcSession();
        Config.save();
        playCloseAnimation(dialog, outer, () -> {
            finished.set(true);
            dialog.dispose();
        });
    }

    private static final class AlphaPanel extends AntiAliasPanel {
        private float alpha = 1f;

        private AlphaPanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        private float getAlpha() {
            return alpha;
        }

        private void setAlpha(float alpha) {
            this.alpha = Math.max(0f, Math.min(1f, alpha));
            repaint();
            if (getParent() != null) {
                getParent().repaint();
            }
        }

        @Override
        public void paint(Graphics graphics) {
            if (!(graphics instanceof Graphics2D g)) {
                super.paint(graphics);
                return;
            }
            Graphics2D copy = (Graphics2D) g.create();
            super.paintComponent(copy);
            super.paintBorder(copy);
            copy.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintChildren(copy);
            copy.dispose();
        }
    }

    private static final class SlidingTabPanel extends AntiAliasPanel {
        private static final int TAB_W = 76;
        private static final int TAB_H = 30;
        private final int tabCount;
        private float indicatorX = 0f;
        private float targetX = 0f;
        private Timer timer;

        private SlidingTabPanel(int tabCount) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            this.tabCount = Math.max(1, tabCount);
            setOpaque(false);
            setPreferredSize(new Dimension(TAB_W * this.tabCount, TAB_H));
            setMinimumSize(new Dimension(TAB_W * this.tabCount, TAB_H));
        }

        private void addTab(JButton button) {
            add(button);
        }

        private void select(int index) {
            targetX = Math.max(0, index) * TAB_W;
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
            long startedAt = System.currentTimeMillis();
            float startX = indicatorX;
            int duration = 180;
            timer = new Timer(16, event -> {
                float t = easeOutCubic((System.currentTimeMillis() - startedAt) / (float) duration);
                indicatorX = startX + (targetX - startX) * t;
                repaint();
                if (t >= 1f) {
                    indicatorX = targetX;
                    timer.stop();
                    repaint();
                }
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!(graphics instanceof Graphics2D g)) {
                return;
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(238, 241, 246));
            g.fillRoundRect(0, 0, TAB_W * tabCount, TAB_H, 10, 10);
            g.setColor(new Color(55, 120, 220));
            g.fillRoundRect(Math.round(indicatorX), 0, TAB_W, TAB_H, 10, 10);
            g.setColor(new Color(205, 210, 220));
            g.drawRoundRect(0, 0, TAB_W * tabCount - 1, TAB_H - 1, 10, 10);
        }
    }

    private static String checkBuildExpiryOnly() {
        try {
            Class<?> gateClass = Class.forName("com.pvp_utils.client.irc.network.IrcBuildGate");
            Method method = gateClass.getDeclaredMethod("checkExpiryOnly");
            method.setAccessible(true);
            Object result = method.invoke(null);
            return result == null ? "" : result.toString();
        } catch (ReflectiveOperationException e) {
            return IrcBridge.MISSING_CORE_MESSAGE;
        }
    }

    private static String checkDeviceBan() {
        try {
            Class<?> clientClass = Class.forName("com.pvp_utils.client.irc.network.PVPUtilsIrcClient");
            Object instance = clientClass.getMethod("getInstance").invoke(null);
            Method method = clientClass.getMethod("checkDeviceBanBlocking", long.class);
            Object result = method.invoke(instance, 5000L);
            return result == null ? "" : result.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
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
                return new UserSummary("", "", "", "无");
            }
            String username = stringMethod(currentUser, "username");
            String role = stringMethod(currentUser, "role");
            String title = stringMethod(currentUser, "title");
            String services = stringMethod(currentUser, "activatedServicesSummary");
            return new UserSummary(username, role, title, services);
        } catch (ReflectiveOperationException ignored) {
            return new UserSummary("", "", "", "无");
        }
    }

    private static String redeemKey(String username, String key) {
        try {
            Class<?> clientClass = Class.forName("com.pvp_utils.client.irc.network.PVPUtilsIrcClient");
            Object instance = clientClass.getMethod("getInstance").invoke(null);
            Method method = clientClass.getMethod("redeemKeyBlocking", String.class, String.class, long.class);
            Object result = method.invoke(instance, username, key, ACTIVATE_TIMEOUT_MS);
            return result == null ? "Activation request failed." : result.toString();
        } catch (ReflectiveOperationException e) {
            return IrcBridge.MISSING_CORE_MESSAGE;
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

    private record UserSummary(String username, String role, String title, String services) {}
}
