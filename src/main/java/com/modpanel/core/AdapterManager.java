package com.modpanel.core;

import com.modpanel.ModPanel;
import com.modpanel.adapter.AdapterContext;
import com.modpanel.adapter.ConfigAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AdapterManager {
    private final List<ConfigAdapter> adapters = new ArrayList<>();

    public void register(ConfigAdapter adapter) {
        adapters.add(adapter);
    }

    public List<ConfigAdapter> getAdapters() {
        return new ArrayList<>(adapters);
    }

    public ConfigAdapter select(AdapterContext context) {
        List<ConfigAdapter> candidates = new ArrayList<>();
        for (ConfigAdapter a : adapters) {
            try {
                if (a.supports(context)) candidates.add(a);
            } catch (Throwable t) {
                ModPanel.LOGGER.warn("Adapter {} failed to support {}", a.descriptor().id(), context.getModId(), t);
            }
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingInt(ConfigAdapter::priority).reversed());
        ConfigAdapter chosen = candidates.get(0);
        List<String> ids = candidates.stream().map(a -> a.descriptor().id()).toList();
        if (candidates.size() > 1 && candidates.get(1).priority() == chosen.priority()) {
            ModPanel.LOGGER.warn("Ambiguous adapters for {}: {}", context.getModId(), ids);
        }
        ModPanel.LOGGER.info("Selected adapter {} for {} (candidates: {})", chosen.descriptor().id(), context.getModId(), ids);
        return chosen;
    }
}
