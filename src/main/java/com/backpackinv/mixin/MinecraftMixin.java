package com.backpackinv.mixin;

import com.backpackinv.client.InventoryAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void backpackinv$interceptInventory(Screen screen, CallbackInfo ci) {
        if (screen instanceof InventoryScreen && InventoryAnimation.handleOpenRequest()) {
            ci.cancel();
        }
    }
}
