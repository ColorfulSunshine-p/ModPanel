package com.modpanel.core;

import com.modpanel.model.ConfigCapabilities;
import com.modpanel.model.ConfigSource;
import com.modpanel.model.DiscoveryResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModSupportTest {
    private ConfigSource source(DiscoveryResult.Status status, boolean writable) {
        return new ConfigSource("m", "m:file", "file", "1",
                new ConfigCapabilities(true, writable, false, false, false, true), status, List.of(), "config/m.json");
    }

    @Test
    void groupsModsBySupport() {
        assertEquals(ModSupport.NO_CONFIG, ModSupport.classify(List.of()));
        assertEquals(ModSupport.SUPPORTED, ModSupport.classify(List.of(source(DiscoveryResult.Status.SUPPORTED, true))));
        assertEquals(ModSupport.READ_ONLY, ModSupport.classify(List.of(source(DiscoveryResult.Status.READ_ONLY, false))));
        assertEquals(ModSupport.NO_CONFIG_FILE, ModSupport.classify(List.of(source(DiscoveryResult.Status.NO_CONFIG, false))));
        assertEquals(ModSupport.INCOMPATIBLE_VERSION,
                ModSupport.classify(List.of(source(DiscoveryResult.Status.INCOMPATIBLE_VERSION, false))));
        assertEquals(ModSupport.ERROR, ModSupport.classify(List.of(source(DiscoveryResult.Status.ERROR, false))));
    }

    @Test
    void sectionsAreOrdered() {
        assertTrue(ModSupport.SUPPORTED.order() < ModSupport.READ_ONLY.order());
        assertTrue(ModSupport.READ_ONLY.order() < ModSupport.PARTIAL.order());
        assertTrue(ModSupport.PARTIAL.order() < ModSupport.NO_CONFIG_FILE.order());
        assertTrue(ModSupport.NO_CONFIG_FILE.order() < ModSupport.UNSUPPORTED_FORMAT.order());
    }
}
