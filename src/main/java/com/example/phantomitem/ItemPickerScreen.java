package com.example.phantomitem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Меню со всеми предметами, иконками и полем поиска. */
public class ItemPickerScreen extends Screen {
    private static final int CELL = 20;
    private static final int GRID_Y = 52;

    private static List<ItemStack> allItems;

    private EditBox search;
    private EditBox countBox;
    private String query = "";
    private int scroll = 0;
    private List<ItemStack> filtered = new ArrayList<>();
    private int columns;
    private int rows;
    private int gridX;

    public ItemPickerScreen() {
        super(Component.translatable("phantomitem.picker.title"));
    }

    private static List<ItemStack> all() {
        if (allItems == null) {
            allItems = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                allItems.add(new ItemStack(item));
            }
        }
        return allItems;
    }

    @Override
    protected void init() {
        columns = Math.max(1, Math.min(14, (this.width - 40) / CELL));
        rows = Math.max(1, (this.height - GRID_Y - 36) / CELL);
        gridX = (this.width - columns * CELL) / 2;

        search = new EditBox(this.font, this.width / 2 - 100, 24, 200, 20,
                Component.translatable("phantomitem.search"));
        search.setValue(query);
        search.setHint(Component.translatable("phantomitem.search"));
        search.setResponder(text -> {
            query = text;
            scroll = 0;
            applyFilter();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        countBox = new EditBox(this.font, this.width / 2 + 106, 24, 44, 20,
                Component.translatable("phantomitem.count_label"));
        countBox.setFilter(text -> text.matches("\\d{0,2}"));
        countBox.setValue(String.valueOf(PhantomConfig.get().count));
        countBox.setResponder(text -> {
            try {
                PhantomConfig.get().count = Math.max(1, Math.min(64, Integer.parseInt(text)));
            } catch (NumberFormatException ignored) {
                // пустое поле: оставляем прежнее значение
            }
        });
        addRenderableWidget(countBox);

        addRenderableWidget(Button.builder(Component.translatable("phantomitem.clear"), b -> GhostItem.clear())
                .bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("phantomitem.settings"),
                        b -> this.minecraft.gui.setScreen(new SettingsScreen(this)))
                .bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());

        applyFilter();
    }

    private void applyFilter() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        filtered = new ArrayList<>();
        for (ItemStack s : all()) {
            if (q.isEmpty()) {
                filtered.add(s);
                continue;
            }
            String name = s.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().replace('_', ' ');
            if (name.contains(q) || id.contains(q)) filtered.add(s);
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        int totalRows = (filtered.size() + columns - 1) / columns;
        return Math.max(0, totalRows - rows);
    }

    private ItemStack stackAt(double mx, double my) {
        int c = (int) Math.floor((mx - gridX) / CELL);
        int r = (int) Math.floor((my - GRID_Y) / CELL);
        if (c < 0 || c >= columns || r < 0 || r >= rows) return null;
        int idx = (scroll + r) * columns + c;
        return idx >= 0 && idx < filtered.size() ? filtered.get(idx) : null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        String t = this.title.getString();
        g.text(this.font, t, (this.width - this.font.width(t)) / 2, 8, 0xFFFFFFFF, true);
        g.text(this.font, Component.translatable("phantomitem.count_label").getString(),
                this.width / 2 + 106, 14, 0xFFAAAAAA, true);

        int start = scroll * columns;
        ItemStack hovered = null;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int idx = start + r * columns + c;
                if (idx >= filtered.size()) break;
                int x = gridX + c * CELL;
                int y = GRID_Y + r * CELL;
                boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
                g.fill(x, y, x + CELL - 1, y + CELL - 1, hover ? 0x80FFFFFF : 0x50000000);
                g.item(filtered.get(idx), x + 1, y + 1);
                if (hover) hovered = filtered.get(idx);
            }
        }

        // Полоса прокрутки.
        int max = maxScroll();
        if (max > 0) {
            int barX = gridX + columns * CELL + 4;
            int barH = rows * CELL;
            g.fill(barX, GRID_Y, barX + 4, GRID_Y + barH, 0x40FFFFFF);
            int thumbH = Math.max(12, barH * rows / (rows + max));
            int thumbY = GRID_Y + (barH - thumbH) * scroll / max;
            g.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xC0FFFFFF);
        }

        String count = Component.translatable("phantomitem.count", filtered.size()).getString();
        g.text(this.font, count, 6, this.height - 22, 0xFFAAAAAA, true);

        if (hovered != null) {
            g.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == 0) {
            ItemStack picked = stackAt(event.x(), event.y());
            if (picked != null) {
                ItemStack give = picked.copy();
                give.setCount(Math.max(1, Math.min(PhantomConfig.get().count, give.getMaxStackSize())));
                PhantomManager.start(give);
                this.onClose();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        PhantomConfig.save();
        super.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) Math.signum(scrollY) * 3;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - step));
        return true;
    }
}
