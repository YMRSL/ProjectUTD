package io.github.ymrsl.firstpersonfoodeating.diagnostic;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.fml.loading.FMLPaths;

public final class BootTrace {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String TOGGLE_PROPERTY = "firstpersonfoodeating.bootTrace";
    private static final long APPLY_TIMEOUT_MS = Long.getLong("firstpersonfoodeating.bootTrace.applyTimeoutMs", 45_000L);
    private static final long WATCHDOG_PERIOD_MS = Long.getLong("firstpersonfoodeating.bootTrace.watchdogPeriodMs", 15_000L);
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean WAITING_APPLY = new AtomicBoolean(false);
    private static volatile long prepareFinishedAtMs = -1L;
    private static volatile long lastDumpAtMs = 0L;
    private static Path traceFile;
    private static ScheduledExecutorService watchdog;

    private BootTrace() {
    }

    public static void init(String reason) {
        if (!isEnabled()) {
            return;
        }
        ensureInitialized();
        writeLine("init", reason);
    }

    public static void event(String stage, String details) {
        if (!isEnabled()) {
            return;
        }
        ensureInitialized();
        writeLine(stage, details);
    }

    public static void error(String stage, Throwable throwable) {
        if (!isEnabled()) {
            return;
        }
        ensureInitialized();
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        writeLine(stage, "exception=" + throwable + "\n" + writer);
    }

    public static void markPrepareStarted() {
        if (!isEnabled()) {
            return;
        }
        ensureInitialized();
        WAITING_APPLY.set(false);
        prepareFinishedAtMs = -1L;
        writeLine("reload.prepare.started", "resource listener prepare started");
    }

    public static void markPrepareFinished(String summary) {
        if (!isEnabled()) {
            return;
        }
        ensureInitialized();
        WAITING_APPLY.set(true);
        prepareFinishedAtMs = System.currentTimeMillis();
        writeLine("reload.prepare.finished", summary);
    }

    public static void markApplyStarted() {
        if (!isEnabled()) {
            return;
        }
        ensureInitialized();
        WAITING_APPLY.set(false);
        writeLine("reload.apply.started", "resource listener apply started");
    }

    public static void markApplyFinished(String summary) {
        if (!isEnabled()) {
            return;
        }
        ensureInitialized();
        WAITING_APPLY.set(false);
        prepareFinishedAtMs = -1L;
        writeLine("reload.apply.finished", summary);
    }

    private static boolean isEnabled() {
        String property = System.getProperty(TOGGLE_PROPERTY);
        if (property == null) {
            return true;
        }
        return Boolean.parseBoolean(property);
    }

    private static synchronized void ensureInitialized() {
        if (STARTED.get()) {
            return;
        }
        try {
            Path logsDir = FMLPaths.GAMEDIR.get().resolve("logs");
            Files.createDirectories(logsDir);
            traceFile = logsDir.resolve("firstpersonfoodeating_boot_trace.log");
            Files.writeString(
                    traceFile,
                    "\n=== " + LocalDateTime.now().format(TIME_FORMAT) + " [" + FirstPersonFoodEatingMod.MOD_ID + "] trace start ===\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            traceFile = null;
        }
        startWatchdog();
        STARTED.set(true);
    }

    private static void startWatchdog() {
        if (watchdog != null) {
            return;
        }
        watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "FPFE-BootTrace-Watchdog");
            thread.setDaemon(true);
            return thread;
        });
        watchdog.scheduleAtFixedRate(BootTrace::checkApplyTimeout, WATCHDOG_PERIOD_MS, WATCHDOG_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    private static void checkApplyTimeout() {
        if (!WAITING_APPLY.get()) {
            return;
        }
        long prepareAt = prepareFinishedAtMs;
        if (prepareAt <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        long pendingMs = now - prepareAt;
        if (pendingMs < APPLY_TIMEOUT_MS) {
            return;
        }
        if (now - lastDumpAtMs < APPLY_TIMEOUT_MS) {
            return;
        }
        lastDumpAtMs = now;
        writeLine("watchdog.timeout", "apply not reached after " + pendingMs + " ms, dumping key thread stacks");
        dumpInterestingThreadStacks();
    }

    private static void dumpInterestingThreadStacks() {
        Map<Thread, StackTraceElement[]> allStacks = Thread.getAllStackTraces();
        List<Map.Entry<Thread, StackTraceElement[]>> selected = new ArrayList<>();
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStacks.entrySet()) {
            Thread thread = entry.getKey();
            String name = thread.getName();
            if (name == null) {
                continue;
            }
            String normalized = name.toLowerCase(Locale.ROOT);
            if (normalized.contains("render thread")
                    || normalized.contains("worker-resourcereload")
                    || normalized.contains("modloading-worker")
                    || normalized.contains("forkjoinpool")
                    || normalized.contains("fpfe-boottrace-watchdog")) {
                selected.add(entry);
            }
        }
        selected.sort(Comparator.comparing(entry -> entry.getKey().getName()));
        for (Map.Entry<Thread, StackTraceElement[]> entry : selected) {
            Thread thread = entry.getKey();
            StringBuilder builder = new StringBuilder();
            builder.append("thread=").append(thread.getName())
                    .append(" state=").append(thread.getState())
                    .append(" daemon=").append(thread.isDaemon());
            StackTraceElement[] stack = entry.getValue();
            int maxDepth = Math.min(stack.length, 24);
            for (int i = 0; i < maxDepth; i++) {
                builder.append("\n  at ").append(stack[i]);
            }
            writeLine("watchdog.thread", builder.toString());
        }
    }

    private static synchronized void writeLine(String stage, String details) {
        String line = String.format(
                "[%s] [%s] [thread=%s] %s%n",
                LocalDateTime.now().format(TIME_FORMAT),
                stage,
                Thread.currentThread().getName(),
                details == null ? "" : details
        );
        try {
            if (traceFile != null) {
                Files.writeString(
                        traceFile,
                        line,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }
        } catch (IOException ignored) {
            // Keep diagnostics best-effort only.
        }
    }
}
