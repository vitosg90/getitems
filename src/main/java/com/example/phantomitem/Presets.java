package com.example.phantomitem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;

/** Наборы частиц и звуков, которые можно выбрать в настройках. "none" = ничего. */
public final class Presets {
    public static final Map<String, ParticleOptions> PARTICLES = new LinkedHashMap<>();
    public static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<>();

    static {
        PARTICLES.put("none", null);
        PARTICLES.put("end_rod", ParticleTypes.END_ROD);
        PARTICLES.put("enchant", ParticleTypes.ENCHANT);
        PARTICLES.put("flame", ParticleTypes.FLAME);
        PARTICLES.put("soul_fire", ParticleTypes.SOUL_FIRE_FLAME);
        PARTICLES.put("portal", ParticleTypes.PORTAL);
        PARTICLES.put("totem", ParticleTypes.TOTEM_OF_UNDYING);
        PARTICLES.put("heart", ParticleTypes.HEART);
        PARTICLES.put("firework", ParticleTypes.FIREWORK);
        PARTICLES.put("witch", ParticleTypes.WITCH);
        PARTICLES.put("happy", ParticleTypes.HAPPY_VILLAGER);
        PARTICLES.put("note", ParticleTypes.NOTE);
        PARTICLES.put("snowflake", ParticleTypes.SNOWFLAKE);
        PARTICLES.put("electric_spark", ParticleTypes.ELECTRIC_SPARK);
        PARTICLES.put("glow", ParticleTypes.GLOW);
        PARTICLES.put("cloud", ParticleTypes.CLOUD);

        SOUNDS.put("none", null);
        SOUNDS.put("chime", SoundEvents.AMETHYST_BLOCK_CHIME);
        SOUNDS.put("enchant", SoundEvents.ENCHANTMENT_TABLE_USE);
        SOUNDS.put("xp", SoundEvents.EXPERIENCE_ORB_PICKUP);
        SOUNDS.put("levelup", SoundEvents.PLAYER_LEVELUP);
        SOUNDS.put("beacon", SoundEvents.BEACON_ACTIVATE);
        SOUNDS.put("ender_eye", SoundEvents.ENDER_EYE_LAUNCH);
        SOUNDS.put("allay", SoundEvents.ALLAY_ITEM_GIVEN);
        SOUNDS.put("totem", SoundEvents.TOTEM_USE);
    }

    private Presets() {
    }

    public static void particle(Level level, String key, double x, double y, double z,
                                double vx, double vy, double vz) {
        ParticleOptions p = PARTICLES.get(key);
        if (p == null) return;
        level.addParticle(p, x, y, z, vx, vy, vz);
    }

    public static void sound(String key, float pitch) {
        SoundEvent ev = SOUNDS.get(key);
        int vol = PhantomConfig.get().volumePercent;
        if (ev == null || vol <= 0) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol / 100f));
    }
}
