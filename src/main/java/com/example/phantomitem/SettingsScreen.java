package com.example.phantomitem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/** Настройки: частицы, звуки, длительность левитации, громкость. */
public class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("phantomitem.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        PhantomConfig c = PhantomConfig.get();
        int cw = 150;
        int lx = this.width / 2 - 155;
        int rx = this.width / 2 + 5;
        int step = 24;
        int y0 = Math.max(26, (this.height - 5 * step - 30) / 2);

        // Левая колонка: частицы и анимация.
        int y = y0;
        addRenderableWidget(cycle(lx, y, cw, "phantomitem.opt.appear_particle", "particle",
                Presets.PARTICLES.keySet(), () -> c.appearParticle, v -> c.appearParticle = v));
        y += step;
        addRenderableWidget(cycle(lx, y, cw, "phantomitem.opt.float_particle", "particle",
                Presets.PARTICLES.keySet(), () -> c.floatParticle, v -> c.floatParticle = v));
        y += step;
        addRenderableWidget(cycle(lx, y, cw, "phantomitem.opt.finish_particle", "particle",
                Presets.PARTICLES.keySet(), () -> c.finishParticle, v -> c.finishParticle = v));
        y += step;
        addRenderableWidget(new IntSlider(lx, y, cw, 1, 5, c.density,
                v -> Component.translatable("phantomitem.opt.density", v), v -> c.density = v));
        y += step;
        addRenderableWidget(new IntSlider(lx, y, cw, 10, 100, c.riseTicks,
                v -> Component.translatable("phantomitem.opt.duration", String.format(Locale.ROOT, "%.1f", v / 20.0)),
                v -> c.riseTicks = v));

        // Правая колонка: звуки.
        y = y0;
        addRenderableWidget(cycle(rx, y, cw, "phantomitem.opt.appear_sound", "sound",
                Presets.SOUNDS.keySet(), () -> c.appearSound, v -> {
                    c.appearSound = v;
                    Presets.sound(v, 1.0f);
                }));
        y += step;
        addRenderableWidget(cycle(rx, y, cw, "phantomitem.opt.finish_sound", "sound",
                Presets.SOUNDS.keySet(), () -> c.finishSound, v -> {
                    c.finishSound = v;
                    Presets.sound(v, 1.2f);
                }));
        y += step;
        addRenderableWidget(new IntSlider(rx, y, cw, 0, 100, c.volumePercent,
                v -> Component.translatable("phantomitem.opt.volume", v), v -> c.volumePercent = v));

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width / 2 - 100, y0 + 5 * step + 8, 200, 20).build());
    }

    private Button cycle(int x, int y, int w, String labelKey, String prefix, Collection<String> keys,
                         Supplier<String> get, Consumer<String> set) {
        List<String> list = new ArrayList<>(keys);
        return Button.builder(cycleLabel(labelKey, prefix, get.get()), b -> {
            int idx = list.indexOf(get.get());
            String next = list.get((idx + 1) % list.size());
            set.accept(next);
            b.setMessage(cycleLabel(labelKey, prefix, next));
        }).bounds(x, y, w, 20).build();
    }

    private static Component cycleLabel(String labelKey, String prefix, String value) {
        return Component.translatable(labelKey, Component.translatable("phantomitem." + prefix + "." + value));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        String t = this.title.getString();
        graphics.text(this.font, t, (this.width - this.font.width(t)) / 2, 10, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        PhantomConfig.save();
        this.minecraft.gui.setScreen(parent);
    }

    /** Ползунок с целым значением и своей подписью. */
    private static class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<Component> label;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, int min, int max, int current,
                  IntFunction<Component> label, IntConsumer setter) {
            super(x, y, w, 20, Component.empty(), (current - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.setter = setter;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            if (label == null) return; // вызов из конструктора родителя
            setMessage(label.apply(current()));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
