package com.modpanel.adapter;

import com.modpanel.compatibility.VersionMatcher;
import com.modpanel.model.*;
import com.modpanel.validation.ValidationResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.List;
import java.util.Properties;

public class LithiumAdapter implements ConfigAdapter {
    public static final String DEFAULT_PATH = "config/lithium.properties";
    private static final String MIN_VERSION = "0.15.0";
    private static final String MAX_VERSION = "0.15.4";
    private static final VersionMatcher VERSION_MATCHER = new VersionMatcher();

    private String fingerprint = "";

    @Override public int priority() { return 100; }

    @Override public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("lithium", List.of("lithium"), List.of("properties"), MIN_VERSION, MAX_VERSION);
    }

    @Override public boolean supports(AdapterContext context) {
        return "lithium".equals(context.getModId())
                && VERSION_MATCHER.matches(context.getModVersion(), MIN_VERSION, MAX_VERSION);
    }

    @Override public DiscoveryResult discover(AdapterContext context) {
        return new DiscoveryResult(DiscoveryResult.Status.SUPPORTED, "Lithium " + context.getModVersion(), List.of());
    }

    @Override public ConfigSnapshot read(ConfigSource source) {
        Path path = pathOf(source);
        Properties defaults = loadDefaults();
        Properties user = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) { user.load(in); }
            catch (Exception ignored) { }
        }
        fingerprint = fingerprint(path);

        List<ConfigEntry> entries = new java.util.ArrayList<>();
        for (String key : defaults.stringPropertyNames()) {
            String def = defaults.getProperty(key, "true");
            String cur = user.getProperty(key, def);
            entries.add(entry(source, key, "true".equalsIgnoreCase(cur), "true".equalsIgnoreCase(def), true));
        }
        for (String key : user.stringPropertyNames()) {
            if (defaults.containsKey(key)) continue;
            String val = user.getProperty(key);
            boolean bool = "true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val);
            entries.add(entry(source, key, bool ? Boolean.parseBoolean(val) : val, null, false));
        }
        return new ConfigSnapshot(source, List.of(), entries);
    }

    @Override public ValidationResult validate(ConfigEntry entry, Object value) {
        if (entry.getType() != SettingType.BOOLEAN) return ValidationResult.fail("Expected boolean");
        return value instanceof Boolean ? ValidationResult.ok() : ValidationResult.fail("Expected boolean");
    }

    @Override public ApplyResult apply(ConfigSource source, List<ConfigChange> changes) {
        Path path = pathOf(source);
        try {
            if (Files.exists(path) && !fingerprint.equals(fingerprint(path))) {
                return new ApplyResult(false, "Conflict: file changed externally since it was read", null, ReloadRequirement.none(), true);
            }
            Properties props = load(path);
            for (ConfigChange c : changes) props.setProperty(key(c.entryId()), String.valueOf(c.proposedValue()));
            writeAtomic(path, props);
            fingerprint = fingerprint(path);
            return new ApplyResult(true, "Saved");
        } catch (Exception e) {
            return new ApplyResult(false, e.getMessage());
        }
    }

    @Override public ResetResult resetToDefault(ConfigSource source, List<String> entryIds) {
        Path path = pathOf(source);
        try {
            Properties props = load(path);
            for (String id : entryIds) props.remove(key(id));
            writeAtomic(path, props);
            fingerprint = fingerprint(path);
            return new ResetResult(true, "Reset to default");
        } catch (Exception e) {
            return new ResetResult(false, e.getMessage());
        }
    }

    @Override public ReloadRequirement reloadRequirement(ConfigSource source, List<ConfigChange> changes) {
        return new ReloadRequirement(true, "Lithium settings require a game restart");
    }

    private ConfigEntry entry(ConfigSource source, String key, Object current, Object def, boolean known) {
        String path = key.startsWith("mixin.") ? key.substring("mixin.".length()) : key;
        String[] parts = path.split("\\.");
        String label = parts[parts.length - 1].replace('_', ' ');
        String category = String.join(".", java.util.Arrays.copyOf(parts, Math.max(parts.length - 1, 1)));
        ConfigCapabilities caps = known ? source.getCapabilities() : ConfigCapabilities.readOnly();
        return new ConfigEntry("lithium:" + key, source.getSourceId(), source.getModId(), key,
                SettingType.BOOLEAN, current, def,
                new SettingMetadata(label, null, category, null, true, !known, false),
                null, null, null, caps);
    }

    private static String key(String entryId) {
        return entryId.startsWith("lithium:") ? entryId.substring("lithium:".length()) : entryId;
    }

    private static Path pathOf(ConfigSource source) {
        Path p = Paths.get(source.getLocation());
        return Files.exists(p) ? p : Paths.get(DEFAULT_PATH);
    }

    private static Properties loadDefaults() {
        Properties defaults = new Properties();
        try (InputStream in = LithiumAdapter.class.getResourceAsStream("/assets/modpanel/lithium-defaults.properties")) {
            if (in != null) defaults.load(in);
        } catch (Exception ignored) { }
        return defaults;
    }

    private static Properties load(Path path) throws Exception {
        Properties props = new Properties();
        if (Files.exists(path)) try (InputStream in = Files.newInputStream(path)) { props.load(in); }
        return props;
    }

    private static void writeAtomic(Path path, Properties props) throws Exception {
        Files.createDirectories(path.getParent());
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp)) { props.store(out, "ModPanel generated overrides"); }
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String fingerprint(Path path) {
        try {
            if (!Files.exists(path)) return "";
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return "";
        }
    }
}
