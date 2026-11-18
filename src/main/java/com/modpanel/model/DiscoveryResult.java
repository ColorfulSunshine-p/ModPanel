package com.modpanel.model;

import java.util.List;

public record DiscoveryResult(
        Status status,
        String message,
        List<String> warnings
) {
    public enum Status {
        SUPPORTED,
        PARTIAL,
        READ_ONLY,
        NO_CONFIG,
        UNSUPPORTED_FORMAT,
        INCOMPATIBLE_VERSION,
        UNSUPPORTED,
        ERROR
    }
}