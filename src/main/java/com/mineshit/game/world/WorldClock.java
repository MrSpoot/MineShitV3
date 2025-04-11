package com.mineshit.game.world;

import com.mineshit.engine.utils.Statistic;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3f;

public class WorldClock {

    private final float cycleDurationSeconds; // Durée d’un cycle complet (ex : 60s)
    private final float startTimeOffset;      // Heure de départ du monde [0,1] (0.25 = minuit, 0.75 = midi)

    @Setter
    private float speed = 1.0f;               // Vitesse du temps (1.0 = normal)
    @Getter
    private boolean paused = false;

    private float pausedTime = 0f;            // Temps figé si pause
    private long lastUpdateMillis;            // Pour calculer le delta

    public WorldClock(float cycleDurationSeconds, float startTimeOffset) {
        this.cycleDurationSeconds = cycleDurationSeconds;
        this.startTimeOffset = startTimeOffset;
        this.lastUpdateMillis = System.currentTimeMillis();
    }

    public void pause() {
        if (!paused) {
            pausedTime = getWorldTime();
            paused = true;
        }
    }

    public void resume() {
        if (paused) {
            lastUpdateMillis = System.currentTimeMillis();
            paused = false;
        }
    }

    public float getWorldTime() {
        if (paused) return pausedTime;

        long now = System.currentTimeMillis();
        float elapsed = (now - lastUpdateMillis) / 1000f;
        float time = startTimeOffset + (elapsed * speed / cycleDurationSeconds);

        Statistic.set("World Time", time);

        return time - (float)Math.floor(time); // garde entre 0-1
    }

    public Vector3f getSunDirection() {
        float angle = -getWorldTime() * 2.0f * (float)Math.PI;
        return new Vector3f((float)Math.cos(angle), (float)Math.sin(angle), 0.0f).normalize();
    }

    public void reset() {
        lastUpdateMillis = System.currentTimeMillis();
        pausedTime = startTimeOffset;
        paused = false;
    }
}

