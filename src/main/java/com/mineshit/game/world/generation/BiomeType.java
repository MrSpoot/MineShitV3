package com.mineshit.game.world.generation;

public enum BiomeType {
    PLAIN,
    DESERT,
    MOUNTAIN,
    OCEAN;

    public static BiomeType fromClimate(float temperature, float humidity) {
        temperature = clamp01((temperature + 1f) / 2f);
        humidity = clamp01((humidity + 1f) / 2f);

        if (humidity > 0.7f) return OCEAN;
        if (temperature > 0.7f && humidity < 0.3f) return DESERT;
        if (temperature < 0.3f && humidity > 0.5f) return MOUNTAIN;
        return PLAIN;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

}
