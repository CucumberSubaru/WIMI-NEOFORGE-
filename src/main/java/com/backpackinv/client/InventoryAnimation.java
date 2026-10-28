package com.backpackinv.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class InventoryAnimation {
    public enum Phase { NONE, RAISING, OPEN, LOWERING }

    private static Phase phase = Phase.NONE;
    private static int ticks = 0;
    private static boolean screenOpened = false;
    private static boolean bypass = false;
    private static float lowerFromBag = 1.0f;
    private static float lowerFromLid = 1.0f;
    private static BackpackInvConfig config = new BackpackInvConfig();
    private static ItemStack propStack;

    /** Корпус и крышка: assets/backpackinv/models/item/backpack_body.json и backpack_lid.json */
    public static final ModelResourceLocation BODY_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("backpackinv", "item/backpack_body"));
    public static final ModelResourceLocation LID_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("backpackinv", "item/backpack_lid"));

    private InventoryAnimation() {}

    public static BackpackInvConfig config() {
        return config;
    }

    public static void reloadConfig() {
        config = BackpackInvConfig.load();
    }

    public static boolean isActive() {
        return phase != Phase.NONE;
    }

    /** Пустышка: рендерер предметов не рисует пустой стек, а сама модель передаётся отдельно. */
    public static ItemStack propStack() {
        if (propStack == null) propStack = new ItemStack(Items.STICK);
        return propStack;
    }

    /**
     * Вызывается, когда игра пытается открыть инвентарь.
     * Возвращает true, если открытие нужно отменить (анимация запущена или уже идёт).
     */
    public static boolean handleOpenRequest() {
        if (bypass || !config.enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isSpectator()) return false;
        switch (phase) {
            case RAISING:
                return true;   // повторное нажатие E во время анимации игнорируем
            case OPEN:
                return false;
            default:
                start();
                return true;
        }
    }

    private static void start() {
        reloadConfig();
        phase = Phase.RAISING;
        ticks = 0;
        screenOpened = false;
    }

    private static void beginLowering() {
        lowerFromBag = bagProgress(0.0f);
        lowerFromLid = lidProgress(0.0f);
        phase = Phase.LOWERING;
        ticks = 0;
    }

    public static void tick(Minecraft mc) {
        if (phase == Phase.NONE) return;
        if (mc.player == null || mc.level == null) {
            phase = Phase.NONE;
            return;
        }
        switch (phase) {
            case RAISING: {
                if (!screenOpened && mc.screen != null) { // открыли что-то другое (меню паузы и т.п.)
                    phase = Phase.NONE;
                    return;
                }
                if (screenOpened && !(mc.screen instanceof InventoryScreen)) { // закрыли инвентарь раньше времени
                    beginLowering();
                    return;
                }
                ticks++;
                int raise = Math.max(1, config.raiseTicks);
                int openTick = Math.max(1, Math.min(raise, Math.round(raise * config.openFraction)));
                if (!screenOpened && ticks >= openTick) {
                    screenOpened = true;
                    bypass = true;
                    try {
                        mc.setScreen(new InventoryScreen(mc.player));
                    } finally {
                        bypass = false;
                    }
                }
                if (ticks >= Math.max(raise, config.raiseTiltTicks)) {
                    phase = Phase.OPEN;
                }
                break;
            }
            case OPEN:
                if (!(mc.screen instanceof InventoryScreen)) {
                    beginLowering();
                }
                break;
            case LOWERING:
                ticks++;
                if (ticks >= Math.max(1, config.lowerTicks)) {
                    phase = Phase.NONE;
                }
                break;
            default:
                break;
        }
    }

    /** Высота рюкзака в кадре: 0 = за кадром, 1 = на месте. Может ненадолго превышать 1 (пружина). */
    public static float bagProgress(float partialTick) {
        switch (phase) {
            case RAISING: {
                float t = clamp01((ticks + partialTick) / Math.max(1, config.raiseTicks));
                float e = easeOutElastic(t);
                // у самого старта пружина не ослабляется, чтобы рюкзак не «прыгал»
                float k = clamp01(t / 0.12f);
                float amp = 1.0f + (config.elasticStrength - 1.0f) * k;
                return 1.0f + (e - 1.0f) * amp;
            }
            case OPEN:
                return 1.0f;
            case LOWERING: {
                float t = clamp01((ticks + partialTick) / Math.max(1, config.lowerTicks));
                return lowerFromBag * (1.0f - t * t * t);
            }
            default:
                return 0.0f;
        }
    }

    /** Открытость крышки: 0 = закрыта, 1 = открыта. */
    public static float lidProgress(float partialTick) {
        switch (phase) {
            case RAISING: {
                float start = Math.max(1, config.raiseTicks) * config.lidStartFraction;
                float t = clamp01((ticks + partialTick - start) / Math.max(1, config.lidTicks));
                return easeOutBack(t);
            }
            case OPEN:
                return 1.0f;
            case LOWERING: {
                float t = clamp01((ticks + partialTick) / Math.max(1.0f, config.lowerTicks * 0.4f));
                return lowerFromLid * (1.0f - t * t * (3.0f - 2.0f * t));
            }
            default:
                return 0.0f;
        }
    }

    /** Насколько сильно рюкзак ещё наклонён при появлении: 1 в начале, 0 когда выровнялся. */
    public static float raiseTilt(float partialTick) {
        if (phase != Phase.RAISING) return 0.0f;
        float t = clamp01((ticks + partialTick) / Math.max(1, config.raiseTiltTicks));
        float u = 1.0f - t;
        return u * u;
    }

    /** Насколько далеко рюкзак ушёл в «убирающее» движение: 0 в начале убирания, 1 в конце. */
    public static float lowerFactor(float partialTick) {
        if (phase != Phase.LOWERING) return 0.0f;
        float t = clamp01((ticks + partialTick) / Math.max(1, config.lowerTicks));
        return t * t;
    }

    private static float easeOutElastic(float t) {
        if (t <= 0.0f) return 0.0f;
        if (t >= 1.0f) return 1.0f;
        double c4 = (2.0 * Math.PI) / 3.0;
        return (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float u = t - 1.0f;
        return 1.0f + c3 * u * u * u + c1 * u * u;
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
