package com.modpanel.client;

import com.modpanel.ModPanel;
import com.modpanel.adapter.AdapterContext;
import com.modpanel.adapter.ConfigAdapter;
import com.modpanel.core.ApplyCoordinator;
import com.modpanel.core.ChangeSet;
import com.modpanel.model.ConfigEntry;
import com.modpanel.model.ConfigSource;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeneratedConfigScreen extends Screen {
    private final String modId;
    private final Screen parentScreen;
    private boolean delegated = false;

    public GeneratedConfigScreen(String modId) {
        this(modId, null);
    }

    public GeneratedConfigScreen(String modId, Screen parent) {
        super(Component.literal("ModPanel - " + modId));
        this.modId = modId;
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (delegated) return;
        delegated = true;

        List<ConfigSource> sources = ModPanel.DISCOVERY.discoverMod(modId, ModPanel.MOD_REGISTRY.getVersion(modId));
        if (sources.isEmpty()) {
            minecraft.setScreen(parentScreen != null ? parentScreen : new ModPanelScreen());
            return;
        }

        AdapterContext context = new AdapterContext(modId, ModPanel.MOD_REGISTRY.getVersion(modId), "1.21.1");
        Map<ConfigSource, ConfigAdapter> adapters = new LinkedHashMap<>();
        Map<String, List<ConfigEntry>> entriesBySource = new LinkedHashMap<>();
        Map<String, ChangeSet> changeSets = new LinkedHashMap<>();
        Map<String, List<ConfigEntry>> grouped = new LinkedHashMap<>();
        boolean writable = false;

        for (ConfigSource source : sources) {
            ConfigAdapter adapter = ModPanel.ADAPTER_MANAGER.select(context);
            if (adapter == null) continue;
            adapters.put(source, adapter);
            changeSets.put(source.getSourceId(), new ChangeSet(source.getSourceId()));
            List<ConfigEntry> entries = adapter.read(source).entries();
            entriesBySource.put(source.getSourceId(), entries);
            writable |= source.getCapabilities().write();
            for (ConfigEntry e : entries) grouped.computeIfAbsent(category(e), k -> new ArrayList<>()).add(e);
        }

        ConfigBuilder builder = ConfigBuilder.create();
        builder.setParentScreen(parentScreen != null ? parentScreen : new ModPanelScreen());
        builder.setTitle(Component.literal(modId + " settings"));
        builder.setEditable(writable);
        builder.setDoesConfirmSave(true);
        if (writable) builder.setSavingRunnable(() -> {
            ApplyCoordinator coordinator = new ApplyCoordinator();
            adapters.forEach((source, adapter) -> {
                if (!source.getCapabilities().write()) return;
                coordinator.apply(adapter, source, changeSets.get(source.getSourceId()),
                        entriesBySource.getOrDefault(source.getSourceId(), List.of()));
            });
        });

        ConfigEntryBuilder eb = builder.entryBuilder();
        for (Map.Entry<String, List<ConfigEntry>> group : grouped.entrySet()) {
            ConfigCategory category = builder.getOrCreateCategory(Component.literal(group.getKey()));
            for (ConfigEntry entry : group.getValue()) addControl(category, eb, entry, changeSets);
            if (group.getValue().stream().anyMatch(e -> e.getMetadata().restartRequired())) {
                category.addEntry(eb.startTextDescription(Component.literal("Restart required for changes in this category")).build());
            }
        }

        if (grouped.isEmpty()) {
            ConfigCategory empty = builder.getOrCreateCategory(Component.literal("No settings"));
            String reason = sources.stream().flatMap(s -> s.getWarnings().stream()).findFirst().orElse(
                    "No supported configuration was found for " + modId);
            empty.addEntry(eb.startTextDescription(Component.literal(reason)).build());
        }

        ConfigCategory status = builder.getOrCreateCategory(Component.literal("Status"));
        for (ConfigSource source : sources) {
            status.addEntry(eb.startTextDescription(Component.literal(
                    source.getSourceId() + " - " + source.getStatus()
                            + (source.getCapabilities().write() ? " (editable)" : " (read-only)")
                            + " - " + source.getLocation())).build());
            for (String warning : source.getWarnings()) {
                status.addEntry(eb.startTextDescription(Component.literal(warning)).build());
            }
        }
        for (var diagnostic : com.modpanel.diagnostics.Diagnostics.entries()) {
            status.addEntry(eb.startTextDescription(Component.literal(
                    diagnostic.category() + ": " + diagnostic.message())).build());
        }

        minecraft.setScreen(builder.build());
    }

    private static String category(ConfigEntry entry) {
        String cat = entry.getMetadata().category();
        return cat == null || cat.isBlank() ? "General" : cat;
    }

    private void addControl(ConfigCategory category, ConfigEntryBuilder eb, ConfigEntry entry, Map<String, ChangeSet> changeSets) {
        String label = entry.getMetadata().label() != null ? entry.getMetadata().label() : entry.getKey();
        Component comp = Component.literal(label);
        ChangeSet changeSet = changeSets.get(entry.getSourceId());
        boolean readOnly = entry.getMetadata().readOnly() || !entry.getCapabilities().write();

        if (readOnly) {
            category.addEntry(eb.startTextDescription(Component.literal(label + ": " + entry.getCurrentValue()
                    + " (read-only)")).build());
            return;
        }

        switch (entry.getType()) {
            case BOOLEAN -> category.addEntry(eb.startBooleanToggle(comp, Boolean.TRUE.equals(entry.getCurrentValue()))
                    .setDefaultValue(() -> Boolean.TRUE.equals(entry.getDefaultValue()))
                    .setErrorSupplier(v -> error(entry, v))
                    .setSaveConsumer(v -> { entry.setCurrentValue(v); stage(changeSet, entry, v); })
                    .build());
            case INTEGER -> {
                var field = eb.startIntField(comp, number(entry).intValue())
                        .setDefaultValue(() -> entry.getDefaultValue() instanceof Number ? ((Number) entry.getDefaultValue()).intValue() : number(entry).intValue())
                        .setErrorSupplier(v -> error(entry, v))
                        .setSaveConsumer(v -> { entry.setCurrentValue(v); stage(changeSet, entry, v); });
                if (entry.getMinValue() instanceof Number min) field.setMin(min.intValue());
                if (entry.getMaxValue() instanceof Number max) field.setMax(max.intValue());
                category.addEntry(field.build());
            }
            case DECIMAL -> {
                var field = eb.startDoubleField(comp, number(entry).doubleValue())
                        .setDefaultValue(() -> entry.getDefaultValue() instanceof Number ? ((Number) entry.getDefaultValue()).doubleValue() : number(entry).doubleValue())
                        .setErrorSupplier(v -> error(entry, v))
                        .setSaveConsumer(v -> { entry.setCurrentValue(v); stage(changeSet, entry, v); });
                if (entry.getMinValue() instanceof Number min) field.setMin(min.doubleValue());
                if (entry.getMaxValue() instanceof Number max) field.setMax(max.doubleValue());
                category.addEntry(field.build());
            }
            case ENUM, STRING -> {
                if (entry.getChoices() != null && !entry.getChoices().isEmpty()) {
                    String[] opts = entry.getChoices().stream().map(String::valueOf).toArray(String[]::new);
                    category.addEntry(eb.startSelector(comp, opts, String.valueOf(entry.getCurrentValue()))
                            .setErrorSupplier(v -> error(entry, v))
                            .setSaveConsumer(v -> { entry.setCurrentValue(v); stage(changeSet, entry, v); })
                            .build());
                } else {
                    category.addEntry(eb.startStrField(comp, String.valueOf(entry.getCurrentValue()))
                            .setErrorSupplier(v -> error(entry, v))
                            .setSaveConsumer(v -> { entry.setCurrentValue(v); stage(changeSet, entry, v); })
                            .build());
                }
            }
            default -> category.addEntry(eb.startTextDescription(Component.literal(label + ": " + entry.getCurrentValue())).build());
        }
    }

    private static void stage(ChangeSet changeSet, ConfigEntry entry, Object value) {
        if (changeSet != null) changeSet.stage(entry, value);
    }

    private static Number number(ConfigEntry entry) {
        return entry.getCurrentValue() instanceof Number n ? n : 0;
    }

    private static Optional<Component> error(ConfigEntry entry, Object value) {
        var result = new com.modpanel.validation.DefaultValidator().validate(entry, value);
        return result.valid() ? Optional.empty() : Optional.of(Component.literal(result.message()));
    }
}
