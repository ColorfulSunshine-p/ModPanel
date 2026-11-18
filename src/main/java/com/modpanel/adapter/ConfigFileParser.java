package com.modpanel.adapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes the configuration files of the supported mods.
 * Every setting, name and description is taken from the file itself, never invented.
 */
public final class ConfigFileParser {
    private static final Pattern ENTRY = Pattern.compile("^(\\s*)([^:=\\s][^:=]*?)(\\s*[:=])(\\s*)(.*)$");
    private static final Pattern DOCUMENTED = Pattern.compile("^#\\s*([\\w.\\-]+)\\s*=\\s*(\\S+)\\s*(#.*)?$");
    private static final Pattern SECTION = Pattern.compile("^\\[([^\\]]+)\\]$");
    private static final Pattern DEFAULT = Pattern.compile("\\(Default:\\s*([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    /**
     * @param key          full key path inside the file
     * @param value        current value
     * @param defaultValue value the mod documents as default, {@code null} when unknown
     * @param description  comment the file documents for this option
     * @param category     section or group the option belongs to
     * @param line         line index of the value, {@code -1} when the option is only documented
     * @param raw          literal text of the value in the file
     */
    public record Setting(String key, Object value, Object defaultValue, String description, String category, int line, String raw) {}

    private ConfigFileParser() {
    }

    public static List<Setting> parse(List<String> lines, ModConfigFile.Format format) {
        return switch (format) {
            case PROPERTIES -> parseProperties(lines);
            case DOCUMENTED -> parseDocumented(lines);
            case TOML -> parseToml(lines);
            case JSON -> parseJson(lines);
        };
    }

    public static String write(List<String> lines, List<Setting> settings, Map<String, Object> values, ModConfigFile.Format format) {
        if (format == ModConfigFile.Format.JSON) return writeJson(lines, values);
        if (format == ModConfigFile.Format.DOCUMENTED) return writeOverrides(lines, settings, values);
        List<String> out = new ArrayList<>(lines);
        for (Setting s : settings) {
            if (!values.containsKey(s.key()) || s.line() < 0) continue;
            out.set(s.line(), replaceValue(out.get(s.line()), formatValue(values.get(s.key()), s, format)));
        }
        return String.join("\n", out);
    }

    public static Object literal(String raw) {
        String value = raw.trim();
        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length() - 1);
        if (value.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (value.equalsIgnoreCase("false")) return Boolean.FALSE;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ignored) {
        }
        return value;
    }

    /** Turns a key of the file into a readable name, e.g. {@code enable_lightmap_caching} -> {@code Enable Lightmap Caching}. */
    public static String humanize(String key) {
        String name = key.substring(key.lastIndexOf('.') + 1).replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('_', ' ').replace('-', ' ').trim();
        StringBuilder sb = new StringBuilder();
        for (String word : name.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private static List<Setting> parseProperties(List<String> lines) {
        List<Setting> out = new ArrayList<>();
        List<String> comment = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.isEmpty()) {
                comment.clear();
                continue;
            }
            if (t.startsWith("#") || t.startsWith("!")) {
                comment.add(t.substring(1).trim());
                continue;
            }
            Matcher m = ENTRY.matcher(lines.get(i));
            if (!m.matches()) {
                comment.clear();
                continue;
            }
            String key = m.group(2).trim();
            String raw = m.group(5).trim();
            out.add(new Setting(key, literal(raw), null, describe(comment), group(key), i, raw));
            comment.clear();
        }
        return out;
    }

    private static List<Setting> parseDocumented(List<String> lines) {
        Map<String, Setting> byKey = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("#")) {
                Matcher m = DOCUMENTED.matcher(t);
                if (m.matches()) {
                    String key = m.group(1);
                    Object def = literal(m.group(2));
                    byKey.put(key, new Setting(key, def, def, note(m.group(3)), group(key), -1, m.group(2)));
                }
                continue;
            }
            Matcher m = ENTRY.matcher(lines.get(i));
            if (!m.matches()) continue;
            String key = m.group(2).trim();
            String raw = m.group(5).trim();
            Setting previous = byKey.get(key);
            byKey.put(key, new Setting(key, literal(raw), previous == null ? null : previous.defaultValue(),
                    previous == null ? null : previous.description(), group(key), i, raw));
        }
        return new ArrayList<>(byKey.values());
    }

    private static List<Setting> parseToml(List<String> lines) {
        List<Setting> out = new ArrayList<>();
        List<String> comment = new ArrayList<>();
        String section = "";
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.isEmpty()) {
                comment.clear();
                continue;
            }
            if (t.startsWith("#")) {
                comment.add(t.substring(1).trim());
                continue;
            }
            Matcher sectionMatcher = SECTION.matcher(t);
            if (sectionMatcher.matches()) {
                section = sectionMatcher.group(1).trim();
                comment.clear();
                continue;
            }
            Matcher m = ENTRY.matcher(lines.get(i));
            if (!m.matches()) {
                comment.clear();
                continue;
            }
            String key = m.group(2).trim();
            String raw = m.group(5).trim();
            String full = section.isEmpty() ? key : section + "." + key;
            String documented = defaultOf(comment);
            Object value = literal(raw);
            if ("default".equalsIgnoreCase(String.valueOf(value)) && documented != null) value = literal(documented);
            out.add(new Setting(full, value, documented == null ? null : literal(documented),
                    describe(comment), section.isEmpty() ? group(full) : section, i, raw));
            comment.clear();
        }
        return out;
    }

    private static List<Setting> parseJson(List<String> lines) {
        List<Setting> out = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(String.join("\n", lines));
            if (root.isJsonObject()) flatten("", root.getAsJsonObject(), out);
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void flatten(String prefix, JsonObject obj, List<Setting> out) {
        obj.entrySet().forEach(e -> {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            JsonElement value = e.getValue();
            if (value.isJsonObject()) flatten(key, value.getAsJsonObject(), out);
            else out.add(new Setting(key, jsonValue(value), null, null, group(key), -1, value.toString()));
        });
    }

    private static Object jsonValue(JsonElement value) {
        if (value.isJsonNull()) return "";
        if (value.isJsonPrimitive()) {
            var p = value.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsString().contains(".") ? (Object) p.getAsDouble() : (Object) p.getAsInt();
            return p.getAsString();
        }
        return value.toString();
    }

    private static String writeOverrides(List<String> lines, List<Setting> settings, Map<String, Object> values) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            Matcher m = ENTRY.matcher(line);
            if (m.matches() && values.containsKey(m.group(2).trim())) continue;
            out.add(line);
        }
        for (Setting s : settings) {
            if (!values.containsKey(s.key())) continue;
            out.add(s.key() + "=" + values.get(s.key()));
        }
        return String.join("\n", out);
    }

    private static String writeJson(List<String> lines, Map<String, Object> values) {
        try {
            JsonObject root = JsonParser.parseString(String.join("\n", lines)).getAsJsonObject();
            for (Map.Entry<String, Object> e : values.entrySet()) set(root, e.getKey(), e.getValue());
            return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);
        } catch (Exception ex) {
            return String.join("\n", lines);
        }
    }

    private static void set(JsonObject root, String path, Object value) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement child = current.get(parts[i]);
            if (child == null || !child.isJsonObject()) {
                child = new JsonObject();
                current.add(parts[i], child);
            }
            current = child.getAsJsonObject();
        }
        String last = parts[parts.length - 1];
        JsonElement element = value instanceof Boolean b ? new com.google.gson.JsonPrimitive(b)
                : value instanceof Number n ? new com.google.gson.JsonPrimitive(n)
                : new com.google.gson.JsonPrimitive(String.valueOf(value));
        current.add(last, element);
    }

    private static String replaceValue(String line, String value) {
        Matcher m = ENTRY.matcher(line);
        if (!m.matches()) return line;
        return m.group(1) + m.group(2) + m.group(3) + m.group(4) + value;
    }

    private static String formatValue(Object value, Setting setting, ModConfigFile.Format format) {
        String text = String.valueOf(value);
        if (format == ModConfigFile.Format.TOML && setting.raw() != null && setting.raw().startsWith("\"")) {
            return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return text;
    }

    private static String defaultOf(List<String> comment) {
        for (String line : comment) {
            Matcher m = DEFAULT.matcher(line);
            if (m.find()) return m.group(1).trim();
        }
        return null;
    }

    private static String describe(List<String> comment) {
        List<String> parts = new ArrayList<>();
        for (String line : comment) {
            String text = line.trim();
            if (text.isEmpty() || text.chars().allMatch(c -> c == '=' || c == '-' || c == '#')) continue;
            if (DEFAULT.matcher(text).find()) continue;
            parts.add(text);
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private static String note(String comment) {
        if (comment == null) return null;
        String text = comment.replace("#", "").trim();
        return text.isEmpty() ? null : text;
    }

    private static String group(String key) {
        if (key.startsWith("mixin.")) {
            String rest = key.substring("mixin.".length());
            int dot = rest.indexOf('.');
            return dot < 0 ? rest : rest.substring(0, dot);
        }
        int dot = key.indexOf('.');
        return dot < 0 ? "General" : key.substring(0, dot);
    }
}
