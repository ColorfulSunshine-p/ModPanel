package com.modpanel.discovery;

import com.modpanel.compatibility.VersionMatcher;
import com.modpanel.model.ConfigCapabilities;
import com.modpanel.model.ConfigSource;
import com.modpanel.model.DiscoveryResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LithiumProvider implements DiscoveryProvider {
    private static final String MIN_VERSION = "0.15.0";
    private static final String MAX_VERSION = "0.15.4";
    private static final VersionMatcher VERSION_MATCHER = new VersionMatcher();

    @Override public String id() { return "lithium"; }

    @Override public int priority() { return 100; }

    @Override public boolean supports(String modId) { return "lithium".equals(modId); }

    @Override public List<ConfigSource> discover(String modId, String version) {
        List<ConfigSource> out = new ArrayList<>();
        boolean compatible = VERSION_MATCHER.matches(version, MIN_VERSION, MAX_VERSION);
        Path path = Paths.get("config", "lithium.properties");
        ConfigCapabilities caps = compatible
                ? new ConfigCapabilities(true, true, true, false, false, true)
                : ConfigCapabilities.readOnly();
        DiscoveryResult.Status status = compatible
                ? DiscoveryResult.Status.SUPPORTED
                : DiscoveryResult.Status.INCOMPATIBLE_VERSION;
        List<String> warnings = compatible ? new ArrayList<>()
                : List.of("Lithium " + version + " is outside the verified range " + MIN_VERSION + " - " + MAX_VERSION);
        out.add(new ConfigSource(modId, "lithium:properties", "properties", version, caps, status, warnings,
                "config/lithium.properties"));
        return out;
    }

    @Override public DiscoveryResult probe(String modId, String version) {
        boolean compatible = VERSION_MATCHER.matches(version, MIN_VERSION, MAX_VERSION);
        return compatible
                ? new DiscoveryResult(DiscoveryResult.Status.SUPPORTED, "Lithium " + version, new ArrayList<>())
                : new DiscoveryResult(DiscoveryResult.Status.INCOMPATIBLE_VERSION,
                        "Lithium " + version + " is outside the verified range " + MIN_VERSION + " - " + MAX_VERSION, new ArrayList<>());
    }
}
