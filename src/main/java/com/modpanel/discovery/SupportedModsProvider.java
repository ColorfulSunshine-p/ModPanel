package com.modpanel.discovery;

import com.modpanel.adapter.ModConfigFile;
import com.modpanel.model.ConfigCapabilities;
import com.modpanel.model.ConfigSource;
import com.modpanel.model.DiscoveryResult;
import com.modpanel.supported.SupportedMods;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Exposes the configuration file of the mods ModPanel supports.
 * Mods without a configuration file are reported as such instead of being given an invented one.
 */
public class SupportedModsProvider implements DiscoveryProvider {
    @Override public String id() { return "supported-mods"; }

    @Override public int priority() { return 50; }

    @Override public boolean supports(String modId) {
        return SupportedMods.find(modId) != null;
    }

    @Override public List<ConfigSource> discover(String modId, String version) {
        List<ConfigSource> out = new ArrayList<>();
        ModConfigFile spec = SupportedMods.find(modId);
        if (spec == null) return out;
        if (!spec.hasFile()) {
            out.add(new ConfigSource(modId, modId + ":none", "none", version, ConfigCapabilities.readOnly(),
                    DiscoveryResult.Status.NO_CONFIG,
                    List.of(spec.displayName() + " does not provide a configuration file"), ""));
            return out;
        }
        Path path = Paths.get(spec.path());
        boolean exists = Files.exists(path);
        ConfigCapabilities caps = new ConfigCapabilities(true, true, resettable(spec), false, false, true);
        DiscoveryResult.Status status = exists ? DiscoveryResult.Status.SUPPORTED : DiscoveryResult.Status.NO_CONFIG;
        List<String> warnings = exists ? List.of() : List.of(spec.path() + " was not created yet by " + spec.displayName());
        out.add(new ConfigSource(modId, modId + ":" + spec.format().name().toLowerCase(), spec.format().name().toLowerCase(),
                version, caps, status, warnings, spec.path()));
        return out;
    }

    @Override public DiscoveryResult probe(String modId, String version) {
        ModConfigFile spec = SupportedMods.find(modId);
        if (spec == null) return new DiscoveryResult(DiscoveryResult.Status.UNSUPPORTED, "No provider", new ArrayList<>());
        if (!spec.hasFile()) {
            return new DiscoveryResult(DiscoveryResult.Status.NO_CONFIG,
                    spec.displayName() + " does not provide a configuration file", new ArrayList<>());
        }
        boolean exists = Files.exists(Paths.get(spec.path()));
        return new DiscoveryResult(exists ? DiscoveryResult.Status.SUPPORTED : DiscoveryResult.Status.NO_CONFIG,
                spec.displayName() + " configuration", new ArrayList<>());
    }

    private static boolean resettable(ModConfigFile spec) {
        return spec.format() == ModConfigFile.Format.DOCUMENTED || spec.format() == ModConfigFile.Format.TOML;
    }
}
