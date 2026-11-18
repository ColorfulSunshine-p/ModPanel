package com.modpanel.model;

import java.util.List;

public class ConfigSource {
    private final String modId;
    private final String sourceId;
    private final String type;
    private final String targetVersion;
    private final ConfigCapabilities capabilities;
    private final DiscoveryResult.Status status;
    private final List<String> warnings;
    private final String location;

    public ConfigSource(String modId, String sourceId, String type, String targetVersion,
                        ConfigCapabilities capabilities, DiscoveryResult.Status status, List<String> warnings) {
        this(modId, sourceId, type, targetVersion, capabilities, status, warnings, sourceId);
    }

    public ConfigSource(String modId, String sourceId, String type, String targetVersion,
                        ConfigCapabilities capabilities, DiscoveryResult.Status status, List<String> warnings, String location) {
        this.modId = modId;
        this.sourceId = sourceId;
        this.type = type;
        this.targetVersion = targetVersion;
        this.capabilities = capabilities;
        this.status = status;
        this.warnings = warnings;
        this.location = location == null ? sourceId : location;
    }

    public String getLocation() { return location; }
    public java.nio.file.Path getPath() { return java.nio.file.Paths.get(location); }

    public String getModId() { return modId; }
    public String getSourceId() { return sourceId; }
    public String getType() { return type; }
    public String getTargetVersion() { return targetVersion; }
    public ConfigCapabilities getCapabilities() { return capabilities; }
    public DiscoveryResult.Status getStatus() { return status; }
    public List<String> getWarnings() { return warnings; }
}