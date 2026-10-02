package com.example.phantomitem;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class PhantomItemClient implements ClientModInitializer {
    public static final String MOD_ID = "phantomitem";

    /** Открывает меню выбора предмета. */
    public static KeyMapping PICKER_KEY;
    /** Повторяет последний выбранный предмет. */
    public static KeyMapping REPEAT_KEY;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        PICKER_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.phantomitem.picker", InputConstants.Type.KEYSYM, InputConstants.KEY_G, category));
        REPEAT_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.phantomitem.repeat", InputConstants.Type.KEYSYM, InputConstants.KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PhantomManager.tick(client);

            while (PICKER_KEY.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    client.gui.setScreen(new ItemPickerScreen());
                }
            }
            while (REPEAT_KEY.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) {
                    if (!PhantomManager.repeatLast()) {
                        client.gui.setScreen(new ItemPickerScreen());
                    }
                }
            }
        });
    }
}
