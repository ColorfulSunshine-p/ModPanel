package com.modpanel.adapter;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class LithiumDefaultsTest {
    @Test
    void bundledDefaultsMatchVerifiedLithiumRelease() throws Exception {
        Properties props = new Properties();
        try (InputStream in = LithiumAdapter.class.getResourceAsStream("/assets/modpanel/lithium-defaults.properties")) {
            assertNotNull(in, "bundled lithium defaults are missing");
            props.load(in);
        }
        assertEquals(150, props.size(), "expected the 150 options of lithium-fabric-0.15.4+mc1.21.1");
        for (String key : props.stringPropertyNames()) {
            assertTrue(key.startsWith("mixin."), key + " is not a Lithium option");
            assertTrue("true".equals(props.getProperty(key)) || "false".equals(props.getProperty(key)), key + " is not boolean");
        }
    }
}
