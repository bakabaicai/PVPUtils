package com.pvp_utils.compatibility;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class JavaLaunchCommand {
    private static final Set<String> VALUE_OPTIONS = Set.of("-classpath", "-cp", "--class-path", "--describe-module", "-d",
            "--source", "--module-path", "-p", "--upgrade-module-path", "--add-modules", "--enable-native-access",
            "--limit-modules", "--add-exports", "--add-opens", "--add-reads", "--patch-module");

    private JavaLaunchCommand() {
    }

    public static List<String> current() throws IOException {
        return current(queryCommand(), Path.of(System.getProperty("user.dir")).toAbsolutePath());
    }

    public static List<String> queryCommand() {
        ProcessHandle handle = ProcessHandle.current();
        ProcessHandle.Info info = handle.info();
        if (info.command().isPresent() && info.arguments().isPresent()) {
            return List.of();
        }
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")
                ? powerShellQuery(handle.pid()) : List.of();
    }

    private static List<String> powerShellQuery(long pid) {
        String query = "$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new(); "
                + "(Get-CimInstance Win32_Process -Filter 'ProcessId = " + pid + "').CommandLine";
        return List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", query);
    }

    public static List<String> current(List<String> approvedQuery, Path workingDirectory) throws IOException {
        ProcessHandle handle = ProcessHandle.current();
        ProcessHandle.Info info = handle.info();
        List<String> command = new ArrayList<>();
        if (!approvedQuery.isEmpty()) {
            if (!approvedQuery.equals(powerShellQuery(handle.pid()))) {
                throw new IOException("The approved Java command query does not match this process");
            }
            Process process = new ProcessBuilder(approvedQuery).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            CompletableFuture<byte[]> captured = CompletableFuture.supplyAsync(() -> {
                try {
                    return process.getInputStream().readNBytes(1_048_576);
                } catch (IOException exception) {
                    return new byte[0];
                }
            });
            try {
                if (!process.waitFor(4, TimeUnit.SECONDS)) throw new IOException("Cannot query the current Java command");
                String line = new String(captured.get(1, TimeUnit.SECONDS), StandardCharsets.UTF_8).strip();
                if (process.exitValue() != 0 || line.isEmpty()) throw new IOException("Current Java command is unavailable");
                command.addAll(parseWindowsCommandLine(line));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Java command query interrupted", exception);
            } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
                throw new IOException("Java command query did not complete", exception);
            } finally {
                process.destroy();
            }
        } else if (info.command().isPresent() && info.arguments().isPresent()) {
            command.add(info.command().orElseThrow());
            command.addAll(Arrays.asList(info.arguments().orElseThrow()));
        } else if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            throw new IOException("The required PowerShell query has not been approved");
        } else if (Files.isRegularFile(Path.of("/proc/self/cmdline"))) {
            String line = new String(Files.readAllBytes(Path.of("/proc/self/cmdline")), Charset.defaultCharset());
            command.addAll(Arrays.asList(line.split("\u0000")));
        } else {
            throw new IOException("The launcher does not expose a reusable Java command");
        }
        if (command.size() < 2) throw new IOException("Incomplete Java launch command");
        return expandArgumentFiles(command, workingDirectory);
    }

    public static List<String> expandArgumentFiles(List<String> command, Path workingDirectory) throws IOException {
        if (command.size() < 2) throw new IOException("Incomplete Java launch command");
        List<String> expanded = new ArrayList<>(List.of(command.getFirst()));
        LauncherState state = new LauncherState();
        for (int index = 1; index < command.size(); index++) {
            String argument = command.get(index);
            if (state.expand && !state.application && argument.startsWith("@@")) {
                append(expanded, state, argument.substring(1));
            } else if (state.expand && !state.application && argument.startsWith("@") && argument.length() > 1) {
                Path file = workingDirectory.resolve(argument.substring(1)).normalize();
                if (Files.size(file) > 16_777_216) throw new IOException("Java argument file is too large");
                Charset encoding = Charset.forName(System.getProperty("native.encoding", Charset.defaultCharset().name()));
                List<String> arguments = parseArgumentFile(Files.readString(file, encoding));
                for (String value : arguments) append(expanded, state, value);
            } else {
                append(expanded, state, argument);
            }
        }
        return List.copyOf(expanded);
    }

    private static void append(List<String> arguments, LauncherState state, String argument) {
        if (!state.application && !state.operand && argument.startsWith("-Dpvp-utils.compatibility.restart.cleanup=")) return;
        arguments.add(argument);
        if (state.application) return;
        if (state.operand) {
            state.operand = false;
            state.application = state.entrypoint;
            return;
        }
        if (argument.equals("--disable-@files")) {
            state.expand = false;
        } else if (argument.equals("-jar") || argument.equals("-m") || argument.equals("--module")) {
            state.operand = true;
            state.entrypoint = true;
        } else if (VALUE_OPTIONS.contains(argument)) {
            state.operand = true;
            state.entrypoint = false;
        } else if (argument.startsWith("--module=") || !argument.startsWith("-")) {
            state.application = true;
        }
    }

    public static List<String> parseWindowsCommandLine(String input) {
        List<String> result = new ArrayList<>();
        int index = 0;
        while (index < input.length()) {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
            if (index == input.length()) break;
            StringBuilder argument = new StringBuilder();
            boolean quoted = false;
            while (index < input.length()) {
                char current = input.charAt(index);
                if (!quoted && Character.isWhitespace(current)) break;
                int slashes = 0;
                while (index < input.length() && input.charAt(index) == '\\') {
                    slashes++;
                    index++;
                }
                if (index < input.length() && input.charAt(index) == '"') {
                    argument.append("\\".repeat(slashes / 2));
                    if ((slashes & 1) != 0) {
                        argument.append('"');
                    } else if (quoted && index + 1 < input.length() && input.charAt(index + 1) == '"') {
                        argument.append('"');
                        index++;
                    } else {
                        quoted = !quoted;
                    }
                    index++;
                } else {
                    argument.append("\\".repeat(slashes));
                    if (index < input.length()) {
                        current = input.charAt(index);
                        if (!quoted && Character.isWhitespace(current)) break;
                        argument.append(current);
                        index++;
                    }
                }
            }
            result.add(argument.toString());
        }
        return List.copyOf(result);
    }

    public static List<String> parseArgumentFile(String input) throws IOException {
        List<String> result = new ArrayList<>();
        StringBuilder argument = new StringBuilder();
        char quote = 0;
        boolean token = false;
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            if (quote == 0 && current == '#') {
                while (index + 1 < input.length() && input.charAt(index + 1) != '\n' && input.charAt(index + 1) != '\r') index++;
            } else if (current == '"' || current == '\'') {
                if (quote == 0) {
                    quote = current;
                    token = true;
                } else if (quote == current) {
                    quote = 0;
                } else {
                    argument.append(current);
                }
            } else if (quote != 0 && current == '\\' && index + 1 < input.length()) {
                char escaped = input.charAt(++index);
                if (escaped == '\n' || escaped == '\r') {
                    if (escaped == '\r' && index + 1 < input.length() && input.charAt(index + 1) == '\n') index++;
                    while (index + 1 < input.length() && Character.isWhitespace(input.charAt(index + 1))) index++;
                } else {
                    argument.append(switch (escaped) {
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'f' -> '\f';
                        default -> escaped;
                    });
                }
            } else if (quote == 0 && Character.isWhitespace(current)) {
                if (token) {
                    result.add(argument.toString());
                    argument.setLength(0);
                    token = false;
                }
            } else {
                argument.append(current);
                token = true;
            }
        }
        if (quote != 0) throw new IOException("Unterminated quote in Java argument file");
        if (token) result.add(argument.toString());
        return List.copyOf(result);
    }

    private static final class LauncherState {
        private boolean expand = true;
        private boolean operand;
        private boolean entrypoint;
        private boolean application;
    }
}
