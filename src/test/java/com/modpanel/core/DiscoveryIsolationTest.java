package com.modpanel.core;

import com.modpanel.discovery.DiscoveryProvider;
import com.modpanel.model.ConfigSource;
import com.modpanel.model.DiscoveryResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryIsolationTest {
    @Test
    void oneFailingProviderDoesNotAbortOthers() {
        DiscoveryCoordinator dc = new DiscoveryCoordinator();
        dc.registerProvider(new DiscoveryProvider() {
            public String id() { return "bad"; }
            public int priority() { return 1; }
            public boolean supports(String modId) { return true; }
            public List<ConfigSource> discover(String modId, String version) { throw new RuntimeException("boom"); }
            public DiscoveryResult probe(String modId, String version) { throw new RuntimeException("boom"); }
        });
        dc.registerProvider(new DiscoveryProvider() {
            public String id() { return "good"; }
            public int priority() { return 1; }
            public boolean supports(String modId) { return true; }
            public List<ConfigSource> discover(String modId, String version) {
                return List.of(new ConfigSource(modId, modId + ":x", "x", version, com.modpanel.model.ConfigCapabilities.readOnly(), DiscoveryResult.Status.READ_ONLY, java.util.List.of()));
            }
            public DiscoveryResult probe(String modId, String version) { return new DiscoveryResult(DiscoveryResult.Status.READ_ONLY, "ok", java.util.List.of()); }
        });
        assertEquals(1, dc.discoverMod("m", "1.0").size());
    }
}
