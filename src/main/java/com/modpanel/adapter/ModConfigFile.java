package com.modpanel.adapter;

/**
 * A configuration file of a supported mod.
 *
 * @param modId       fabric mod id owning the file
 * @param displayName human readable mod name
 * @param path        file location relative to the game directory, {@code null} when the mod has no configuration file
 * @param format      file layout
 */
public record ModConfigFile(String modId, String displayName, String path, Format format) {
    public enum Format { PROPERTIES, DOCUMENTED, TOML, JSON }

    public boolean hasFile() {
        return path != null && !path.isBlank();
    }
}
