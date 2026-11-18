package com.modpanel.adapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.modpanel.model.*;
import com.modpanel.validation.ValidationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericJsonAdapter implements ConfigAdapter {
    @Override public int priority() { return 1; }
    @Override public AdapterDescriptor descriptor() {
        return new AdapterDescriptor("generic-json", List.of(), List.of("json"), null, null);
    }
    @Override public boolean supports(AdapterContext context) {
        return !com.modpanel.supported.SupportedMods.isHandled(context.getModId())
                && Files.exists(Paths.get("config", context.getModId() + ".json"));
    }
    @Override public DiscoveryResult discover(AdapterContext context) {
        return new DiscoveryResult(DiscoveryResult.Status.READ_ONLY, "Generic json", new ArrayList<>());
    }
    @Override public ConfigSnapshot read(ConfigSource source) {
        List<ConfigEntry> entries = new ArrayList<>();
        Path path = Paths.get(source.getLocation());
        if (!Files.exists(path)) path = Paths.get("config", source.getModId() + ".json");
        String text = "";
        try { text = Files.readString(path); } catch (IOException ignored) { }
        try {
            JsonElement root = JsonParser.parseString(text);
            if (root.isJsonObject()) flatten(source, "", root.getAsJsonObject(), entries);
        } catch (Exception ignored) { }
        return new ConfigSnapshot(source, new ArrayList<>(), entries);
    }
    @Override public ValidationResult validate(ConfigEntry e, Object v) { return ValidationResult.ok(); }
    @Override public ApplyResult apply(ConfigSource s, List<ConfigChange> c) { return new ApplyResult(false, "Read-only adapter"); }
    @Override public ResetResult resetToDefault(ConfigSource s, List<String> ids) { return new ResetResult(false, "Read-only adapter"); }
    @Override public ReloadRequirement reloadRequirement(ConfigSource s, List<ConfigChange> c) { return ReloadRequirement.none(); }

    private void flatten(ConfigSource source, String prefix, JsonObject obj, List<ConfigEntry> entries) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonElement v = e.getValue();
            if (v.isJsonObject()) flatten(source, key, v.getAsJsonObject(), entries);
            else entries.add(toEntry(source, key, v));
        }
    }

    private ConfigEntry toEntry(ConfigSource source, String key, JsonElement v) {
        SettingType type; Object val;
        if (v.isJsonNull()) { type = SettingType.UNKNOWN; val = null; }
        else if (v.isJsonPrimitive()) {
            var p = v.getAsJsonPrimitive();
            if (p.isBoolean()) { type = SettingType.BOOLEAN; val = p.getAsBoolean(); }
            else if (p.isNumber()) {
                String s = p.getAsString();
                if (s.contains(".")) { type = SettingType.DECIMAL; val = p.getAsDouble(); }
                else { type = SettingType.INTEGER; val = p.getAsInt(); }
            } else { type = SettingType.STRING; val = p.getAsString(); }
        } else if (v.isJsonArray()) { type = SettingType.COLLECTION; val = toList(v.getAsJsonArray()); }
        else { type = SettingType.UNKNOWN; val = v.toString(); }
        return new ConfigEntry(source.getModId() + ":" + key, source.getSourceId(), source.getModId(), key,
                type, val, null, new SettingMetadata(key, null, null, null, false, true, false),
                null, null, null, ConfigCapabilities.readOnly());
    }

    private List<Object> toList(com.google.gson.JsonArray arr) {
        List<Object> out = new ArrayList<>();
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive()) {
                var p = e.getAsJsonPrimitive();
                if (p.isBoolean()) out.add(p.getAsBoolean());
                else if (p.isNumber()) {
                    String s = p.getAsString();
                    if (s.contains(".")) out.add(p.getAsDouble()); else out.add(p.getAsInt());
                } else out.add(p.getAsString());
            } else out.add(e.toString());
        }
        return out;
    }
}
