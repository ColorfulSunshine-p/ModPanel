package com.modpanel.adapter;

import com.modpanel.model.ConfigEntry;
import com.modpanel.model.ConfigSource;
import com.modpanel.model.DiscoveryResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenericJsonAdapterTest {
    @Test
    void parsesNestedAndEscaped() throws Exception {
        Files.createDirectories(Path.of("config"));
        Files.writeString(Path.of("config", "jm.json"), "{\"a\":{\"b\":true},\"c\":\"x\\\\y\",\"d\":[1,2]}");
        ConfigSource src = new ConfigSource("jm", "jm:json", "json", "1", com.modpanel.model.ConfigCapabilities.readOnly(), DiscoveryResult.Status.READ_ONLY, List.of(), "config/jm.json");
        GenericJsonAdapter a = new GenericJsonAdapter();
        var snap = a.read(src);
        assertTrue(snap.entries().size() >= 3);
        Files.deleteIfExists(Path.of("config", "jm.json"));
    }
}
