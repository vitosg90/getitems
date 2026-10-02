package com.example.phantomitem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Призрачный предмет: подменяем предмет в руке только на клиенте.
 * Сервер о нём не знает, поэтому он пропадёт, когда сервер обновит инвентарь.
 */
public final class GhostItem {
    private static ItemStack ghost;
    private static ItemStack previous;

    private GhostItem() {
    }

    public static void give(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        // Запоминаем настоящий предмет, только если в руке сейчас не наш призрак.
        if (ghost == null || !ItemStack.matches(ghost, player.getMainHandItem())) {
            previous = player.getMainHandItem().copy();
        }
        ghost = stack.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ghost.copy());
    }

    /** Убирает призрак и возвращает в руку настоящий предмет (если призрак ещё там). */
    public static void clear() {
        var player = Minecraft.getInstance().player;
        if (player != null && ghost != null && ItemStack.matches(ghost, player.getMainHandItem())) {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous == null ? ItemStack.EMPTY : previous);
        }
        ghost = null;
        previous = null;
    }
}
