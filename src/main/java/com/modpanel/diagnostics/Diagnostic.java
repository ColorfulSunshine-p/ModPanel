package com.modpanel.diagnostics;

public record Diagnostic(ErrorCategory category, String message, Throwable cause) {
    public static Diagnostic of(ErrorCategory c, String m) { return new Diagnostic(c, m, null); }
}
