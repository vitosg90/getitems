package com.example.phantomitem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Настройки мода. Хранятся в config/phantomitem.json. */
public class PhantomConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PhantomConfig instance;

    /** Сколько штук предмета дать (ограничивается размером стака предмета). */
    public int count = 1;
    /** Длительность левитации в тиках (20 тиков = 1 секунда). */
    public int riseTicks = 40;
    /** Количество частиц, 1..5. */
    public int density = 2;
    public int volumePercent = 100;

    public String appearParticle = "end_rod";
    public String floatParticle = "end_rod";
    public String finishParticle = "end_rod";
    public String appearSound = "enchant";
    public String finishSound = "chime";

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("phantomitem.json");
    }

    public static PhantomConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        PhantomConfig loaded = null;
        Path f = file();
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f)) {
                loaded = GSON.fromJson(r, PhantomConfig.class);
            } catch (Exception e) {
                System.err.println("[phantomitem] Не удалось прочитать конфиг: " + e);
            }
        }
        instance = loaded != null ? loaded : new PhantomConfig();
        instance.fix();
    }

    public static void save() {
        if (instance == null) return;
        instance.fix();
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            System.err.println("[phantomitem] Не удалось сохранить конфиг: " + e);
        }
    }

    private void fix() {
        count = Math.max(1, Math.min(64, count));
        riseTicks = Math.max(10, Math.min(100, riseTicks));
        density = Math.max(1, Math.min(5, density));
        volumePercent = Math.max(0, Math.min(100, volumePercent));
        if (!Presets.PARTICLES.containsKey(appearParticle)) appearParticle = "end_rod";
        if (!Presets.PARTICLES.containsKey(floatParticle)) floatParticle = "end_rod";
        if (!Presets.PARTICLES.containsKey(finishParticle)) finishParticle = "end_rod";
        if (!Presets.SOUNDS.containsKey(appearSound)) appearSound = "enchant";
        if (!Presets.SOUNDS.containsKey(finishSound)) finishSound = "chime";
    }
}
