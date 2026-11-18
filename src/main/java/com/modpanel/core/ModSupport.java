package com.modpanel.core;

import com.modpanel.model.ConfigSource;
import com.modpanel.model.DiscoveryResult;

import java.util.List;

/**
 * How well a mod is supported, used to group the mod list into sections.
 */
public enum ModSupport {
    SUPPORTED("Supported", 0),
    OWN_SCREEN("Has its own configuration screen", 1),
    READ_ONLY("Supported (read only)", 2),
    PARTIAL("Partially supported", 3),
    NO_CONFIG_FILE("No configuration file", 4),
    UNSUPPORTED_FORMAT("Unsupported format", 5),
    INCOMPATIBLE_VERSION("Incompatible version", 6),
    NO_CONFIG("No configuration found", 7),
    ERROR("Error", 8);

    private final String title;
    private final int order;

    ModSupport(String title, int order) {
        this.title = title;
        this.order = order;
    }

    public String title() {
        return title;
    }

    public int order() {
        return order;
    }

    public static ModSupport classify(List<ConfigSource> sources) {
        if (sources == null || sources.isEmpty()) return NO_CONFIG;
        if (sources.stream().anyMatch(s -> s.getCapabilities().write())) return SUPPORTED;
        DiscoveryResult.Status status = sources.get(0).getStatus();
        return switch (status) {
            case READ_ONLY -> READ_ONLY;
            case PARTIAL -> PARTIAL;
            case NO_CONFIG -> NO_CONFIG_FILE;
            case UNSUPPORTED_FORMAT -> UNSUPPORTED_FORMAT;
            case INCOMPATIBLE_VERSION -> INCOMPATIBLE_VERSION;
            case ERROR -> ERROR;
            default -> NO_CONFIG;
        };
    }
}
