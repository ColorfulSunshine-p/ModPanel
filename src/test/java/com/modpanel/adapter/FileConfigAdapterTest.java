package com.modpanel.adapter;

import com.modpanel.model.ConfigCapabilities;
import com.modpanel.model.ConfigChange;
import com.modpanel.model.ConfigSource;
import com.modpanel.model.DiscoveryResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileConfigAdapterTest {
    private Path fixture(String name) throws Exception {
        Path target = Files.createTempFile(name, ".cfg");
        Files.copy(Path.of("src/test/resources/fixtures", name), target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private ConfigSource source(Path path, String modId) {
        return new ConfigSource(modId, modId + ":file", "file", "1", new ConfigCapabilities(true, true, true, false, false, true),
                DiscoveryResult.Status.SUPPORTED, List.of(), path.toString());
    }

    @Test
    void generatesSettingsFromTheFileOfTheMod() throws Exception {
        Path file = fixture("badoptimizations.txt");
        FileConfigAdapter adapter = new FileConfigAdapter(
                new ModConfigFile("badoptimizations", "BadOptimizations", file.toString(), ModConfigFile.Format.PROPERTIES));
        var entries = adapter.read(source(file, "badoptimizations")).entries();
        var lightmap = entries.stream().filter(e -> e.getKey().equals("enable_lightmap_caching")).findFirst().orElseThrow();
        assertEquals("Enable Lightmap Caching", lightmap.getMetadata().label());
        assertTrue(lightmap.getMetadata().description().contains("lightmap"));
        assertEquals(com.modpanel.model.SettingType.BOOLEAN, lightmap.getType());
        Files.deleteIfExists(file);
    }

    @Test
    void writesTheValueAndKeepsTheRestOfTheFile() throws Exception {
        Path file = fixture("badoptimizations.txt");
        FileConfigAdapter adapter = new FileConfigAdapter(
                new ModConfigFile("badoptimizations", "BadOptimizations", file.toString(), ModConfigFile.Format.PROPERTIES));
        adapter.read(source(file, "badoptimizations"));
        var result = adapter.apply(source(file, "badoptimizations"),
                List.of(new ConfigChange("badoptimizations:enable_lightmap_caching", true, false)));
        assertTrue(result.success(), result.message());
        String written = Files.readString(file);
        assertTrue(written.contains("enable_lightmap_caching: false"), written);
        assertTrue(written.contains("# Whether we should cancel updating the lightmap if not needed."));
        assertTrue(written.contains("config_version: 6"), written);
        Files.deleteIfExists(file);
    }

    @Test
    void detectsExternalChanges() throws Exception {
        Path file = fixture("badoptimizations.txt");
        FileConfigAdapter adapter = new FileConfigAdapter(
                new ModConfigFile("badoptimizations", "BadOptimizations", file.toString(), ModConfigFile.Format.PROPERTIES));
        adapter.read(source(file, "badoptimizations"));
        Files.writeString(file, Files.readString(file) + "\nextra_option=true\n");
        var result = adapter.apply(source(file, "badoptimizations"),
                List.of(new ConfigChange("badoptimizations:enable_lightmap_caching", true, false)));
        assertFalse(result.success());
        assertTrue(result.conflict());
        Files.deleteIfExists(file);
    }

    @Test
    void modernFixOverridesAreAppendedToTheFile() throws Exception {
        Path file = fixture("modernfix-mixins.properties");
        FileConfigAdapter adapter = new FileConfigAdapter(
                new ModConfigFile("modernfix", "ModernFix", file.toString(), ModConfigFile.Format.DOCUMENTED));
        adapter.read(source(file, "modernfix"));
        var result = adapter.apply(source(file, "modernfix"),
                List.of(new ConfigChange("modernfix:mixin.perf.chunk_meshing", true, false)));
        assertTrue(result.success(), result.message());
        String written = Files.readString(file);
        assertTrue(written.contains("mixin.perf.chunk_meshing=false"), written);
        assertTrue(written.contains("# Available options:"));
        Files.deleteIfExists(file);
    }
}
