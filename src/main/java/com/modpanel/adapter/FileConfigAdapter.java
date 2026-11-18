package com.modpanel.adapter;

import com.modpanel.compatibility.VersionMatcher;
import com.modpanel.model.*;
import com.modpanel.persistence.BackupService;
import com.modpanel.validation.ValidationResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the configuration screen of a mod from the configuration file the mod itself provides.
 * Settings, names and descriptions come from that file only.
 */
public class FileConfigAdapter implements ConfigAdapter {
    private final ModConfigFile spec;
    private String fingerprint = "";

    public FileConfigAdapter(ModConfigFile spec) {
        this.spec = spec;
    }

    @Override public int priority() { return 50; }

    @Override public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("file-" + spec.modId(), List.of(spec.modId()),
                List.of(spec.format().name().toLowerCase()), null, null);
    }

    @Override public boolean supports(AdapterContext context) {
        return spec.modId().equals(context.getModId());
    }

    @Override public DiscoveryResult discover(AdapterContext context) {
        if (!spec.hasFile()) {
            return new DiscoveryResult(DiscoveryResult.Status.NO_CONFIG,
                    spec.displayName() + " does not provide a configuration file", List.of());
        }
        return new DiscoveryResult(DiscoveryResult.Status.SUPPORTED, spec.displayName() + " configuration", List.of());
    }

    @Override public ConfigSnapshot read(ConfigSource source) {
        Path path = pathOf(source);
        List<ConfigFileParser.Setting> settings = path == null ? List.of() : parse(path);
        fingerprint = path == null ? "" : fingerprint(path);

        List<ConfigEntry> entries = new ArrayList<>();
        for (ConfigFileParser.Setting s : settings) {
            boolean complex = isComplex(s);
            entries.add(new ConfigEntry(spec.modId() + ":" + s.key(), source.getSourceId(), source.getModId(), s.key(),
                    complex ? SettingType.COLLECTION : type(s.value()), s.value(), s.defaultValue(),
                    new SettingMetadata(ConfigFileParser.humanize(s.key()), s.description(), s.category(), null, true, complex, false),
                    null, null, null, complex || isInternal(s.key()) ? ConfigCapabilities.readOnly() : source.getCapabilities()));
        }
        return new ConfigSnapshot(source, List.of(), entries);
    }

    @Override public ValidationResult validate(ConfigEntry entry, Object value) {
        return switch (entry.getType()) {
            case BOOLEAN -> value instanceof Boolean ? ValidationResult.ok() : ValidationResult.fail("Expected a boolean value");
            case INTEGER -> value instanceof Integer || value instanceof Long
                    ? ValidationResult.ok() : ValidationResult.fail("Expected a whole number");
            case DECIMAL -> value instanceof Number ? ValidationResult.ok() : ValidationResult.fail("Expected a number");
            case STRING -> value instanceof String ? ValidationResult.ok() : ValidationResult.fail("Expected text");
            default -> ValidationResult.ok();
        };
    }

    @Override public ApplyResult apply(ConfigSource source, List<ConfigChange> changes) {
        Path path = pathOf(source);
        if (path == null) return new ApplyResult(false, "This mod does not provide a configuration file");
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (!fingerprint.isEmpty() && !fingerprint.equals(fingerprint(path))) {
                return new ApplyResult(false, "Conflict: the file changed externally since it was read", null,
                        ReloadRequirement.none(), true);
            }
            List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(lines, spec.format());
            Map<String, Object> values = new LinkedHashMap<>();
            for (ConfigChange c : changes) {
                String key = key(c.entryId());
                ConfigFileParser.Setting s = settings.stream().filter(x -> x.key().equals(key)).findFirst().orElse(null);
                if (s == null || isComplex(s) || isInternal(s.key())) continue;
                values.put(key, c.proposedValue());
            }
            Path backup = new BackupService().backup(source.getSourceId(), path);
            write(path, ConfigFileParser.write(lines, settings, values, spec.format()));
            fingerprint = fingerprint(path);
            return new ApplyResult(true, "Saved", backup == null ? null : backup.toString(),
                    new ReloadRequirement(true, spec.displayName() + " reads its configuration at startup"), false);
        } catch (Exception e) {
            return new ApplyResult(false, "Could not save: " + e.getMessage());
        }
    }

    @Override public ResetResult resetToDefault(ConfigSource source, List<String> entryIds) {
        Path path = pathOf(source);
        if (path == null) return new ResetResult(false, "This mod does not provide a configuration file");
        if (spec.format() != ModConfigFile.Format.DOCUMENTED && spec.format() != ModConfigFile.Format.TOML) {
            return new ResetResult(false, "This mod does not document default values for " + entryIds.size() + " setting(s)");
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(lines, spec.format());
            Map<String, Object> values = new LinkedHashMap<>();
            for (String id : entryIds) {
                ConfigFileParser.Setting s = settings.stream().filter(x -> x.key().equals(key(id))).findFirst().orElse(null);
                if (s == null) continue;
                values.put(s.key(), spec.format() == ModConfigFile.Format.TOML ? "default" : s.defaultValue());
            }
            write(path, ConfigFileParser.write(lines, settings, values, spec.format()));
            fingerprint = fingerprint(path);
            return new ResetResult(true, "Reset to default");
        } catch (Exception e) {
            return new ResetResult(false, e.getMessage());
        }
    }

    @Override public ReloadRequirement reloadRequirement(ConfigSource source, List<ConfigChange> changes) {
        return new ReloadRequirement(true, spec.displayName() + " reads its configuration at startup");
    }

    private static SettingType type(Object value) {
        if (value instanceof Boolean) return SettingType.BOOLEAN;
        if (value instanceof Integer || value instanceof Long) return SettingType.INTEGER;
        if (value instanceof Number) return SettingType.DECIMAL;
        return SettingType.STRING;
    }

    /** Lists and objects of a file cannot be edited safely, they are shown read-only. */
    private static boolean isComplex(ConfigFileParser.Setting setting) {
        String raw = setting.raw();
        return raw != null && (raw.startsWith("[") || raw.startsWith("{"));
    }

    /** Version markers belong to the mod and must not be changed. */
    private static boolean isInternal(String key) {
        return key.equals("configVersion") || key.equals("version");
    }

    private String key(String entryId) {
        String prefix = spec.modId() + ":";
        return entryId.startsWith(prefix) ? entryId.substring(prefix.length()) : entryId;
    }

    private Path pathOf(ConfigSource source) {
        String location = source.getLocation();
        if (location == null || location.isBlank()) return null;
        Path path = Paths.get(location);
        return Files.exists(path) ? path : null;
    }

    private List<ConfigFileParser.Setting> parse(Path path) {
        try {
            return ConfigFileParser.parse(Files.readAllLines(path, StandardCharsets.UTF_8), spec.format());
        } catch (Exception e) {
            return List.of();
        }
    }

    private static void write(Path path, String content) throws Exception {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String fingerprint(Path path) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (Exception e) {
            return "";
        }
    }
}
