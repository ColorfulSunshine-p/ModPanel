package com.modpanel.adapter;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.modpanel.model.*;
import com.modpanel.validation.ValidationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericTomlAdapter implements ConfigAdapter {
    @Override public int priority() { return 1; }
    @Override public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("generic-toml", List.of(), List.of("toml"), null, null);
    }
    @Override public boolean supports(AdapterContext context) {
        return !com.modpanel.supported.SupportedMods.isHandled(context.getModId())
                && Files.exists(Paths.get("config", context.getModId() + ".toml"));
    }
    @Override public DiscoveryResult discover(AdapterContext context) {
        return new DiscoveryResult(DiscoveryResult.Status.READ_ONLY, "Generic toml", new ArrayList<>());
    }
    @Override public ConfigSnapshot read(ConfigSource source) {
        List<ConfigEntry> entries = new ArrayList<>();
        Path path = Paths.get(source.getLocation());
        if (!Files.exists(path)) path = Paths.get("config", source.getModId() + ".toml");
        try {
            Config cfg = TomlFormat.instance().createParser().parse(path, com.electronwill.nightconfig.core.file.FileNotFoundAction.READ_NOTHING);
            flatten(source, "", cfg, entries);
        } catch (Exception ignored) { }
        return new ConfigSnapshot(source, new ArrayList<>(), entries);
    }
    @Override public ValidationResult validate(ConfigEntry e, Object v) { return ValidationResult.ok(); }
    @Override public ApplyResult apply(ConfigSource s, List<ConfigChange> c) { return new ApplyResult(false, "Read-only adapter"); }
    @Override public ResetResult resetToDefault(ConfigSource s, List<String> ids) { return new ResetResult(false, "Read-only adapter"); }
    @Override public ReloadRequirement reloadRequirement(ConfigSource s, List<ConfigChange> c) { return ReloadRequirement.none(); }

    private void flatten(ConfigSource source, String prefix, Config cfg, List<ConfigEntry> entries) {
        for (Config.Entry e : cfg.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof Config c) flatten(source, key, c, entries);
            else entries.add(toEntry(source, key, v));
        }
    }

    private ConfigEntry toEntry(ConfigSource source, String key, Object v) {
        SettingType type; Object val = v;
        if (v instanceof Boolean) type = SettingType.BOOLEAN;
        else if (v instanceof Long || v instanceof Integer) type = SettingType.INTEGER;
        else if (v instanceof Double || v instanceof Float) type = SettingType.DECIMAL;
        else if (v instanceof List) { type = SettingType.COLLECTION; val = v; }
        else type = SettingType.STRING;
        return new ConfigEntry(source.getModId() + ":" + key, source.getSourceId(), source.getModId(), key,
                type, val, null, new SettingMetadata(key, null, null, null, false, true, false),
                null, null, null, ConfigCapabilities.readOnly());
    }
}
