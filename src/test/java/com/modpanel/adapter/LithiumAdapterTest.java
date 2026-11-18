package com.modpanel.adapter;

import com.modpanel.model.ConfigCapabilities;
import com.modpanel.model.ConfigChange;
import com.modpanel.model.ConfigEntry;
import com.modpanel.model.ConfigSource;
import com.modpanel.model.DiscoveryResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LithiumAdapterTest {
    private static ConfigSource source(Path path) {
        return new ConfigSource("lithium", "lithium:properties", "properties", "0.15.4",
                new ConfigCapabilities(true, true, true, false, false, true),
                DiscoveryResult.Status.SUPPORTED, List.of(), path.toString());
    }

    @Test
    void readsVerifiedKeysAndKeepsUnknownReadOnly() throws Exception {
        Path file = Files.createTempFile("lithium", ".properties");
        Files.writeString(file, "mixin.ai=true\ncustom.unknown=42\n");
        LithiumAdapter adapter = new LithiumAdapter();
        var entries = adapter.read(source(file)).entries();
        assertEquals(150, entries.stream().filter(e -> e.getCapabilities().write()).count());
        ConfigEntry unknown = entries.stream().filter(e -> e.getKey().equals("custom.unknown")).findFirst().orElseThrow();
        assertFalse(unknown.getCapabilities().write());
        assertTrue(unknown.getMetadata().readOnly());
        Files.deleteIfExists(file);
    }

    @Test
    void applyPreservesUnknownKeysAndDetectsConflicts() throws Exception {
        Path file = Files.createTempFile("lithium", ".properties");
        Files.writeString(file, "mixin.ai=true\ncustom.unknown=42\n");
        LithiumAdapter adapter = new LithiumAdapter();
        adapter.read(source(file));
        assertTrue(adapter.apply(source(file), List.of(new ConfigChange("lithium:mixin.ai", true, false))).success());
        assertTrue(Files.readString(file).contains("custom.unknown=42"));
        assertTrue(Files.readString(file).contains("mixin.ai=false"));

        Files.writeString(file, "mixin.ai=true\ncustom.unknown=42\nexternal=1\n");
        assertFalse(adapter.apply(source(file), List.of(new ConfigChange("lithium:mixin.ai", true, false))).success());
        Files.deleteIfExists(file);
    }
}
