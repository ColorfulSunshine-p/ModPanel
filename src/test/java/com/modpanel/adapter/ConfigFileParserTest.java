package com.modpanel.adapter;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigFileParserTest {
    private List<String> lines(String fixture) throws Exception {
        return Files.readAllLines(Path.of("src/test/resources/fixtures", fixture));
    }

    @Test
    void readsBadOptimizationsOptionsAndDescriptions() throws Exception {
        List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(lines("badoptimizations.txt"), ModConfigFile.Format.PROPERTIES);
        ConfigFileParser.Setting lightmap = settings.stream()
                .filter(s -> s.key().equals("enable_lightmap_caching")).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, lightmap.value());
        assertEquals("Enable Lightmap Caching", ConfigFileParser.humanize(lightmap.key()));
        assertTrue(lightmap.description().contains("lightmap"), lightmap.description());
        assertTrue(settings.stream().anyMatch(s -> s.key().equals("config_version")));
    }

    @Test
    void readsFerriteCoreOptions() throws Exception {
        List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(lines("ferritecore.mixin.properties"), ModConfigFile.Format.PROPERTIES);
        assertEquals(12, settings.size());
        ConfigFileParser.Setting detector = settings.stream()
                .filter(s -> s.key().equals("useSmallThreadingDetector")).findFirst().orElseThrow();
        assertEquals(Boolean.FALSE, detector.value());
        assertEquals("Use Small Threading Detector", ConfigFileParser.humanize(detector.key()));
    }

    @Test
    void readsModernFixDocumentedOptions() throws Exception {
        List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(lines("modernfix-mixins.properties"), ModConfigFile.Format.DOCUMENTED);
        assertEquals(64, settings.size());
        ConfigFileParser.Setting chunkMeshing = settings.stream()
                .filter(s -> s.key().equals("mixin.perf.chunk_meshing")).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, chunkMeshing.value());
        assertEquals(Boolean.TRUE, chunkMeshing.defaultValue());
        assertEquals("perf", chunkMeshing.category());
    }

    @Test
    void readsC2meTomlSectionsAndDefaults() throws Exception {
        List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(lines("c2me.toml"), ModConfigFile.Format.TOML);
        ConfigFileParser.Setting enabled = settings.stream()
                .filter(s -> s.key().equals("clientSideConfig.modifyMaxVDConfig.enabled")).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, enabled.value());
        assertEquals(Boolean.TRUE, enabled.defaultValue());
        assertEquals("clientSideConfig.modifyMaxVDConfig", enabled.category());
        assertTrue(settings.stream().anyMatch(s -> s.key().equals("generalOptimizations.autoSave.mode")));
    }

    @Test
    void readsEntityCullingJson() throws Exception {
        List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(lines("entityculling.json"), ModConfigFile.Format.JSON);
        assertTrue(settings.stream().anyMatch(s -> s.key().equals("safeMode") && Boolean.TRUE.equals(s.value())));
        assertTrue(settings.stream().anyMatch(s -> s.key().equals("tracingDistance") && Integer.valueOf(128).equals(s.value())));
        assertTrue(settings.stream().anyMatch(s -> s.key().equals("tickCullingWhitelist")));
    }

    @Test
    void writesValuesAndKeepsComments() throws Exception {
        List<String> original = lines("badoptimizations.txt");
        List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(original, ModConfigFile.Format.PROPERTIES);
        String written = ConfigFileParser.write(original, settings, java.util.Map.of("enable_lightmap_caching", false),
                ModConfigFile.Format.PROPERTIES);
        assertTrue(written.contains("enable_lightmap_caching: false"), written);
        assertTrue(written.contains("# Whether we should cancel updating the lightmap if not needed."));
        assertTrue(written.contains("lightmap_time_change_needed_for_update: 80"));
    }

    @Test
    void writesModernFixOverrides() throws Exception {
        List<String> original = lines("modernfix-mixins.properties");
        List<ConfigFileParser.Setting> settings = ConfigFileParser.parse(original, ModConfigFile.Format.DOCUMENTED);
        String written = ConfigFileParser.write(original, settings, java.util.Map.of("mixin.perf.chunk_meshing", false),
                ModConfigFile.Format.DOCUMENTED);
        assertTrue(written.contains("mixin.perf.chunk_meshing=false"), written);
        assertTrue(written.contains("# Available options:"));
        assertFalse(written.contains("\nmixin.perf.chunk_meshing=true\n"), written);
    }
}
