package com.modpanel.compatibility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VersionMatcherSemverTest {
    @Test
    void prereleaseAndBoundary() {
        VersionMatcher vm = new VersionMatcher();
        assertTrue(vm.matches("1.2.3", "1.0.0", "2.0.0"));
        assertFalse(vm.matches("1.0.0-rc", "1.0.0", "2.0.0"));
        assertTrue(vm.matches("1.0.0", "1.0.0-rc", "2.0.0"));
        assertTrue(vm.compare("1.0.0", "1.0.0-rc") > 0);
    }

    @Test
    void ignoresBuildMetadata() {
        VersionMatcher vm = new VersionMatcher();
        assertTrue(vm.matches("0.15.4+mc1.21.1", "0.15.0", "0.15.4"));
        assertFalse(vm.matches("0.15.5+mc1.21.1", "0.15.0", "0.15.4"));
        assertTrue(vm.matches("0.15.4+mc1.21.1", "0.15.4", null));
    }
}
