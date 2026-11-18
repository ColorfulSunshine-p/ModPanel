package com.modpanel.client;

import com.modpanel.ModPanel;
import com.modpanel.core.ModRegistry;
import com.modpanel.core.ModSupport;
import com.modpanel.model.ConfigSource;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModPanelScreen extends Screen {
    private static final int GAP = 6;
    private static final int MARGIN = 10;
    private static final int HEADER_HEIGHT = 20;
    private static final int ENTRY_HEIGHT = 26;
    private static final int MIN_COLUMN_WIDTH = 170;
    private static final int MAX_COLUMN_WIDTH = 260;

    private final Screen parentScreen;
    private EditBox searchBox;
    private String query = "";
    private final List<Column> columns = new ArrayList<>();

    public ModPanelScreen() {
        this(null);
    }

    public ModPanelScreen(Screen parent) {
        super(Component.literal("ModPanel"));
        this.parentScreen = parent;
    }

    private record Entry(ModRegistry.ModInfo mod, String detail, boolean openable) {}

    private static final class Column {
        private final ModSupport support;
        private final List<Entry> entries;
        private int scroll;

        private Column(ModSupport support, List<Entry> entries) {
            this.support = support;
            this.entries = entries;
        }
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(this.font, this.width / 2 - 140, 30, 280, 18, Component.literal("Search"));
        searchBox.setResponder(s -> { query = s; refresh(); });
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("Close"), b -> {
            if (parentScreen != null) minecraft.setScreen(parentScreen);
            else onClose();
        }).bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());

        refresh();
    }

    private void refresh() {
        Map<ModSupport, List<Entry>> grouped = new EnumMap<>(ModSupport.class);
        for (ModRegistry.ModInfo mod : ModPanel.MOD_REGISTRY.getLoadedMods()) {
            if (mod.id().startsWith("fabric") || mod.id().equals("java") || mod.id().equals("minecraft")
                    || mod.id().equals("mixinextras") || mod.id().equals("modpanel")) continue;
            if (!query.isBlank() && !mod.name().toLowerCase().contains(query.toLowerCase())
                    && !mod.id().toLowerCase().contains(query.toLowerCase())) continue;
            List<ConfigSource> sources = ModPanel.DISCOVERY.discoverMod(mod.id(), mod.version());
            grouped.computeIfAbsent(supportOf(mod, sources), k -> new ArrayList<>())
                    .add(new Entry(mod, detail(mod.id(), sources), !sources.isEmpty()));
        }
        columns.clear();
        for (ModSupport support : ModSupport.values()) {
            List<Entry> entries = grouped.get(support);
            if (entries != null && !entries.isEmpty()) columns.add(new Column(support, entries));
        }
    }

    private ModSupport supportOf(ModRegistry.ModInfo mod, List<ConfigSource> sources) {
        ModSupport support = ModSupport.classify(sources);
        return support == ModSupport.NO_CONFIG && hasOwnConfigUi(mod.id()) ? ModSupport.OWN_SCREEN : support;
    }

    private String detail(String modId, List<ConfigSource> sources) {
        if (sources.isEmpty()) return hasOwnConfigUi(modId) ? "provided by Mod Menu" : "no configuration found";
        ConfigSource source = sources.get(0);
        if (!source.getWarnings().isEmpty()) return source.getWarnings().get(0);
        return source.getCapabilities().write()
                ? source.getLocation()
                : source.getStatus().name().toLowerCase().replace('_', ' ');
    }

    private boolean hasOwnConfigUi(String modId) {
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) return false;
        try {
            for (EntrypointContainer<ModMenuApi> c : FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class)) {
                if (!modId.equals(c.getProvider().getMetadata().getId())) continue;
                ModMenuApi api = c.getEntrypoint();
                if (api.getModConfigScreenFactory() != null) return true;
                Map<String, ConfigScreenFactory<?>> provided = api.getProvidedConfigScreenFactories();
                if (provided != null && provided.containsKey(modId)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private int columnsPerRow() {
        return Math.max(1, (this.width - 2 * MARGIN + GAP) / (MIN_COLUMN_WIDTH + GAP));
    }

    private int columnWidth() {
        int perRow = columnsPerRow();
        return Math.max(MIN_COLUMN_WIDTH, Math.min(MAX_COLUMN_WIDTH,
                (this.width - 2 * MARGIN - GAP * (perRow - 1)) / perRow));
    }

    private int contentTop() {
        return 58;
    }

    private int contentBottom() {
        return this.height - 36;
    }

    private int rowHeight() {
        int rows = Math.max(1, (columns.size() + columnsPerRow() - 1) / columnsPerRow());
        return Math.max(HEADER_HEIGHT + ENTRY_HEIGHT, (contentBottom() - contentTop() - GAP * (rows - 1)) / rows);
    }

    /** x, y, width and height of the column at the given index. */
    private int[] rectOf(int index) {
        int perRow = columnsPerRow();
        int width = columnWidth();
        int height = rowHeight();
        return new int[]{MARGIN + (index % perRow) * (width + GAP), contentTop() + (index / perRow) * (height + GAP), width, height};
    }

    private int maxScroll(Column column) {
        int visible = rowHeight() - HEADER_HEIGHT - 2;
        return Math.max(0, column.entries.size() * ENTRY_HEIGHT - visible);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF1A1A1A);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, "ModPanel", this.width / 2, 10, 0xFFFFFF);

        for (int i = 0; i < columns.size(); i++) {
            int[] rect = rectOf(i);
            drawColumn(graphics, columns.get(i), rect, mouseX, mouseY);
        }
    }

    private void drawColumn(GuiGraphics graphics, Column column, int[] rect, int mouseX, int mouseY) {
        int x = rect[0], y = rect[1], width = rect[2], height = rect[3];
        int bodyTop = y + HEADER_HEIGHT + 1;
        int bodyBottom = y + height;

        graphics.fill(x, y, x + width, bodyBottom, 0x60000000);

        int visible = height - HEADER_HEIGHT - 2;
        int offset = Math.min(column.scroll, maxScroll(column));
        int y0 = bodyTop - offset;
        for (Entry entry : column.entries) {
            if (y0 + ENTRY_HEIGHT > bodyTop && y0 < bodyBottom) {
                boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y0 && mouseY < y0 + ENTRY_HEIGHT;
                if (hovered) graphics.fill(x, y0, x + width, y0 + ENTRY_HEIGHT, 0x40FFFFFF);
                String name = fit(entry.mod.name(), width - 12);
                graphics.drawString(this.font, name, x + 6, y0 + 3, 0xFFFFFF);
                int room = width - 12 - this.font.width(name) - 8;
                if (room > 16) {
                    graphics.drawString(this.font, fit(entry.mod.version(), room),
                            x + 6 + this.font.width(name) + 6, y0 + 3, 0x9A9A9A);
                }
                graphics.drawString(this.font, fit(entry.detail(), width - 18), x + 10, y0 + 14, 0xB0B0B0);
            }
            y0 += ENTRY_HEIGHT;
        }

        graphics.fill(x, y, x + width, y + HEADER_HEIGHT, 0xFF3A3A3A);
        graphics.fill(x, y, x + width, y + 1, 0xFF909090);
        graphics.fill(x, y + HEADER_HEIGHT - 1, x + width, y + HEADER_HEIGHT, 0xFF222222);
        graphics.drawString(this.font, fit(column.support.title() + " (" + column.entries.size() + ")", width - 8),
                x + 5, y + 6, 0xFFFFFF);

        int overflow = maxScroll(column);
        if (overflow > 0) {
            int barHeight = Math.max(20, visible * visible / (visible + overflow));
            int barTop = bodyTop + (visible - barHeight) * offset / overflow;
            graphics.fill(x + width - 3, bodyTop, x + width - 1, bodyBottom, 0x40000000);
            graphics.fill(x + width - 3, barTop, x + width - 1, barTop + barHeight, 0xFFB4B4B4);
        }
    }

    private String fit(String text, int width) {
        String cut = this.font.plainSubstrByWidth(text, Math.max(10, width - 8));
        return cut.length() < text.length() ? cut + "…" : cut;
    }

    private Column columnAt(double mouseX, double mouseY) {
        for (int i = 0; i < columns.size(); i++) {
            int[] rect = rectOf(i);
            if (mouseX >= rect[0] && mouseX < rect[0] + rect[2] && mouseY >= rect[1] && mouseY < rect[1] + rect[3]) {
                return columns.get(i);
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < columns.size(); i++) {
            int[] rect = rectOf(i);
            if (mouseX < rect[0] || mouseX >= rect[0] + rect[2] || mouseY < rect[1] || mouseY >= rect[1] + rect[3]) continue;
            Column column = columns.get(i);
            int index = (int) ((mouseY - (rect[1] + HEADER_HEIGHT + 1) + column.scroll) / ENTRY_HEIGHT);
            if (index < 0 || index >= column.entries.size()) return true;
            Entry entry = column.entries.get(index);
            if (entry.openable()) minecraft.setScreen(new GeneratedConfigScreen(entry.mod.id(), this));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Column column = columnAt(mouseX, mouseY);
        if (column == null) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        int before = column.scroll;
        column.scroll = Math.max(0, Math.min(maxScroll(column), column.scroll - (int) (scrollY * 12)));
        return before != column.scroll;
    }
}
