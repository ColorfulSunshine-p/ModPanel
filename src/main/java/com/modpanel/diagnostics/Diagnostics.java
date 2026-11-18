package com.modpanel.diagnostics;

import com.modpanel.ModPanel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class Diagnostics {
    private static final int MAX = 50;
    private static final Deque<Diagnostic> ENTRIES = new ArrayDeque<>();

    private Diagnostics() {
    }

    public static void report(ErrorCategory category, String message, Throwable cause) {
        ENTRIES.addLast(new Diagnostic(category, message, cause));
        while (ENTRIES.size() > MAX) ENTRIES.removeFirst();
        if (cause == null) ModPanel.LOGGER.warn("[{}] {}", category, message);
        else ModPanel.LOGGER.warn("[{}] {}", category, message, cause);
    }

    public static List<Diagnostic> entries() {
        return new ArrayList<>(ENTRIES);
    }

    public static String export() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        StringBuilder sb = new StringBuilder("ModPanel diagnostics\n");
        for (Diagnostic d : ENTRIES) {
            sb.append(LocalDateTime.now().format(fmt)).append(" [").append(d.category()).append("] ")
                    .append(d.message());
            if (d.cause() != null) sb.append(" (").append(d.cause().getClass().getSimpleName()).append(")");
            sb.append('\n');
        }
        return sb.toString();
    }
}
