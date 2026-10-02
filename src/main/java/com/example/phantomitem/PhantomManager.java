package com.example.phantomitem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Анимация: предмет появляется перед игроком (частицы и звук), левитирует,
 * затем улетает в руку, где остаётся призрачная копия.
 * Предмет в мире это клиентская сущность с отрицательным id, сервер её не видит.
 */
public final class PhantomManager {
    private static final int FLY_TICKS = 10;

    private static ItemEntity entity;
    private static ItemStack stack;
    private static ItemStack lastStack;
    private static int ticks;
    private static int riseTicks = 40;
    private static Vec3 flyStart = Vec3.ZERO;
    private static int fakeId = -2_000_000;

    private PhantomManager() {
    }

    public static boolean repeatLast() {
        if (lastStack == null) return false;
        start(lastStack.copy());
        return true;
    }

    public static void start(ItemStack chosen) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        remove();

        PhantomConfig cfg = PhantomConfig.get();
        riseTicks = cfg.riseTicks;
        stack = chosen.copy();
        lastStack = chosen.copy();
        ticks = 0;

        Vec3 pos = floatPos(mc.player, 0f);
        flyStart = pos;
        entity = new ItemEntity(mc.level, pos.x, pos.y, pos.z, stack.copy());
        entity.setId(fakeId--);
        entity.setNoGravity(true);
        entity.setNeverPickUp();
        entity.setUnlimitedLifetime();
        entity.setDeltaMovement(Vec3.ZERO);
        mc.level.addEntity(entity);

        appearBurst(mc.level, pos, cfg);
        Presets.sound(cfg.appearSound, 1.0f);
    }

    public static void tick(Minecraft mc) {
        if (entity == null) return;
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null || entity.isRemoved()) {
            remove();
            return;
        }
        PhantomConfig cfg = PhantomConfig.get();
        ticks++;

        if (ticks <= riseTicks) {
            Vec3 pos = floatPos(player, ticks / (float) riseTicks);
            move(pos);
            floatParticles(level, pos, cfg);
            flyStart = pos;
        } else if (ticks <= riseTicks + FLY_TICKS) {
            float q = (ticks - riseTicks) / (float) FLY_TICKS;
            Vec3 target = handPos(player);
            Vec3 pos = flyStart.add(target.subtract(flyStart).scale(q * q));
            move(pos);
            trailParticles(level, pos, cfg);
        } else {
            finishBurst(level, handPos(player), cfg);
            Presets.sound(cfg.finishSound, 1.2f);
            ItemStack given = stack.copy();
            remove();
            GhostItem.give(given);
        }
    }

    private static void move(Vec3 pos) {
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    private static void remove() {
        if (entity != null) {
            entity.discard();
            entity = null;
        }
    }

    private static Vec3 forward(Player player) {
        Vec3 look = player.getViewVector(1.0f);
        double hx = look.x;
        double hz = look.z;
        double len = Math.sqrt(hx * hx + hz * hz);
        if (len < 1e-4) {
            return new Vec3(0, 0, 1);
        }
        return new Vec3(hx / len, 0, hz / len);
    }

    /** Левитация: предмет висит перед игроком и плавно поднимается. */
    private static Vec3 floatPos(Player player, float progress) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 f = forward(player);
        double ease = 1 - (1 - progress) * (1 - progress);
        double wobble = Math.sin(ticks * 0.3) * 0.05;
        return eye.add(f.x * 1.9, -0.9 + 0.8 * ease + wobble, f.z * 1.9);
    }

    /** Примерное место правой руки. */
    private static Vec3 handPos(Player player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 f = forward(player);
        double rx = -f.z;
        double rz = f.x;
        return eye.add(f.x * 0.55 + rx * 0.45, -0.6, f.z * 0.55 + rz * 0.45);
    }

    /** Частицы появления: кольцо вокруг предмета и столб вверх. */
    private static void appearBurst(Level level, Vec3 pos, PhantomConfig cfg) {
        var rnd = ThreadLocalRandom.current();
        int n = 10 * cfg.density;
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            Presets.particle(level, cfg.appearParticle,
                    pos.x + Math.cos(a) * 0.2, pos.y + 0.15, pos.z + Math.sin(a) * 0.2,
                    Math.cos(a) * 0.12, 0.03, Math.sin(a) * 0.12);
        }
        for (int i = 0; i < 4 * cfg.density; i++) {
            Presets.particle(level, cfg.appearParticle,
                    pos.x + (rnd.nextDouble() - 0.5) * 0.3, pos.y + rnd.nextDouble() * 0.2, pos.z + (rnd.nextDouble() - 0.5) * 0.3,
                    0, 0.06 + rnd.nextDouble() * 0.05, 0);
        }
    }

    private static void floatParticles(Level level, Vec3 pos, PhantomConfig cfg) {
        var rnd = ThreadLocalRandom.current();
        for (int i = 0; i < cfg.density; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double r = 0.3 + rnd.nextDouble() * 0.25;
            Presets.particle(level, cfg.floatParticle,
                    pos.x + Math.cos(a) * r, pos.y + 0.15 + rnd.nextDouble() * 0.5, pos.z + Math.sin(a) * r,
                    0, 0.015, 0);
        }
        // Два «спутника», кружащихся вокруг предмета.
        double base = ticks * 0.55;
        for (int i = 0; i < 2; i++) {
            double ang = base + i * Math.PI;
            Presets.particle(level, cfg.floatParticle,
                    pos.x + Math.cos(ang) * 0.5, pos.y + 0.3 + 0.2 * Math.sin(ticks * 0.2), pos.z + Math.sin(ang) * 0.5,
                    0, 0.01, 0);
        }
    }

    private static void trailParticles(Level level, Vec3 pos, PhantomConfig cfg) {
        var rnd = ThreadLocalRandom.current();
        for (int i = 0; i < cfg.density + 1; i++) {
            Presets.particle(level, cfg.floatParticle,
                    pos.x + (rnd.nextDouble() - 0.5) * 0.2, pos.y + 0.2 + (rnd.nextDouble() - 0.5) * 0.2,
                    pos.z + (rnd.nextDouble() - 0.5) * 0.2, 0, 0, 0);
        }
    }

    private static void finishBurst(Level level, Vec3 pos, PhantomConfig cfg) {
        var rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 5 * cfg.density; i++) {
            Presets.particle(level, cfg.finishParticle, pos.x, pos.y + 0.2, pos.z,
                    (rnd.nextDouble() - 0.5) * 0.25, (rnd.nextDouble() - 0.3) * 0.25, (rnd.nextDouble() - 0.5) * 0.25);
        }
    }
}
