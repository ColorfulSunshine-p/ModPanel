package com.modpanel.supported;

import com.modpanel.adapter.ModConfigFile;

import java.util.List;

/**
 * Configuration files of the mods ModPanel supports.
 * Every entry was verified by inspecting the mod itself: the file name comes from the mod code
 * and the settings are read from that file at runtime.
 */
public final class SupportedMods {
    public static final List<ModConfigFile> FILES = List.of(
            new ModConfigFile("badoptimizations", "BadOptimizations", "config/badoptimizations.txt", ModConfigFile.Format.PROPERTIES),
            new ModConfigFile("ferritecore", "FerriteCore", "config/ferritecore.mixin.properties", ModConfigFile.Format.PROPERTIES),
            new ModConfigFile("modernfix", "ModernFix", "config/modernfix-mixins.properties", ModConfigFile.Format.DOCUMENTED),
            new ModConfigFile("c2me", "C2ME", "config/c2me.toml", ModConfigFile.Format.TOML),
            new ModConfigFile("entityculling", "EntityCulling", "config/entityculling.json", ModConfigFile.Format.JSON),
            new ModConfigFile("scalablelux", "ScalableLux", "config/scalablelux.properties", ModConfigFile.Format.PROPERTIES),
            new ModConfigFile("krypton", "Krypton", null, null),
            new ModConfigFile("noisium", "Noisium", null, null)
    );

    private SupportedMods() {
    }

    public static ModConfigFile find(String modId) {
        return FILES.stream().filter(f -> f.modId().equals(modId)).findFirst().orElse(null);
    }

    /** True when the mod has its own configuration file and must not be handled by the generic adapters. */
    public static boolean isHandled(String modId) {
        ModConfigFile spec = find(modId);
        return spec != null && spec.hasFile();
    }
}
