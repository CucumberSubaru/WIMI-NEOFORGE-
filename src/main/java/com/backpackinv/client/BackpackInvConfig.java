package com.backpackinv.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Настройки лежат в файле config/backpackinv-config.json.
 * Файл перечитывается при каждом открытии инвентаря, перезапуск игры не нужен.
 * Чтобы вернуть значения по умолчанию, удалите файл.
 */
public class BackpackInvConfig {
    public boolean enabled = true;

    // ---- время (20 тиков = 1 секунда) ----
    public int raiseTicks = 13;          // общая длительность появления рюкзака
    public float openFraction = 0.4f;    // в какой момент появления (0..1) открывается сам инвентарь
    public int lowerTicks = 10;          // длительность убирания рюкзака
    public float elasticStrength = 0.2f; // сила «пружины» (0 = без отскока, 1 = как в оригинальной easeOutElastic)

    // ---- наклон при появлении и убирании ----
    // ---- доставание: те же параметры, что у убирания, но движение играется в обратную сторону ----
    public float raiseTiltAngle = 0.0f;     // наклон вперёд-назад в начале (градусы)
    public float raiseRollAngle = 25.0f;    // наклон вбок в начале (+ против часовой, - по часовой)
    public float raiseShiftX = 1.8f;        // смещение вбок в начале (+ вправо, - влево)
    public int raiseTiltTicks = 10;         // длительность выравнивания (тиков, 20 = секунда)
    public float lowerTiltAngle = -50.0f;     // наклон, с которым рюкзак уходит вниз при убирании
    public float lowerRollAngle = -25.0f;     // поворот по часовой (-) или против (+) при убирании
    public float lowerShiftX = 0.5f;         // смещение вбок при убирании (+ вправо, - влево)

    // ---- крышка ----
    public float lidOpenAngle = 85.0f;   // насколько откидывается крышка (градусы)
    public float lidStartFraction = 0.35f; // в какой момент появления (0..1) начинает открываться крышка
    public int lidTicks = 8;             // сколько тиков открывается крышка

    // ---- рюкзак ----
    public boolean useVanillaTilt = false; // true = наклон как у карты в ванили (зависит от взгляда)
    public float fixedTilt = 0.35f;
    public float scale = 1.0f;
    public float offsetX = 0.0f;
    public float offsetY = -0.3f;
    public float offsetZ = 0.0f;
    public float rotX = 0.0f;
    public float rotY = 0.0f;
    public float rotZ = 0.0f;

    // ---- руки ----
    public boolean showHands = true;
    public float handInward = 0.04f;     // сдвиг рук к центру (+) или наружу (-)
    public float handUp = 0.2f;          // сдвиг рук вверх (+) или вниз (-)
    public float handForward = 0.0f;     // сдвиг рук к камере (+) или от камеры (-)
    public float handExtraRotX = 0.0f;   // дополнительные повороты рук (градусы)
    public float handExtraRotY = 0.0f;
    public float handExtraRotZ = 0.0f;

    // дополнительное движение ПРАВОЙ руки, когда она открывает крышку
    public float openHandUp = 0.15f;
    public float openHandBack = -0.10f;
    public float openHandRotX = -20.0f;

    public static BackpackInvConfig load() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path path = FMLPaths.CONFIGDIR.get().resolve("backpackinv-config.json");
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    BackpackInvConfig loaded = gson.fromJson(reader, BackpackInvConfig.class);
                    if (loaded != null) {
                        return loaded;
                    }
                }
            }
            BackpackInvConfig def = new BackpackInvConfig();
            Files.writeString(path, gson.toJson(def));
            return def;
        } catch (Exception e) {
            e.printStackTrace();
            return new BackpackInvConfig();
        }
    }
}
