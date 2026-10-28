package com.backpackinv;

import com.backpackinv.client.InventoryAnimation;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Мод только клиентский: на сервере этот класс не создаётся, регистрировать предметы не нужно. */
@Mod(value = BackpackInv.MOD_ID, dist = Dist.CLIENT)
public class BackpackInv {
    public static final String MOD_ID = "wini";

    public BackpackInv(IEventBus modBus) {
        InventoryAnimation.reloadConfig();
        modBus.addListener(BackpackInv::registerModels);
        NeoForge.EVENT_BUS.addListener(BackpackInv::onClientTick);
    }

    /** Корпус и крышка рюкзака загружаются как самостоятельные модели (без предметов). */
    private static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(InventoryAnimation.BODY_MODEL);
        event.register(InventoryAnimation.LID_MODEL);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        InventoryAnimation.tick(Minecraft.getInstance());
    }
}
