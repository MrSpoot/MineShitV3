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
            long now = System.currentTimeMillis();
            float elapsedRatio = pausedTime - startTimeOffset;
            if (elapsedRatio < 0) elapsedRatio += 1.0f;

            lastUpdateMillis = now - (long)(elapsedRatio * cycleDurationSeconds * 1000f / speed);
            paused = false;
        }
    }

    public float getWorldTime() {
        if (paused) return pausedTime;

        long now = System.currentTimeMillis();
        float elapsed = (now - lastUpdateMillis) / 1000f;
        float time = startTimeOffset + (elapsed * speed / cycleDurationSeconds);

        Statistic.set("World Time", time);

        return time - (float)Math.floor(time);
    }

    public Vector3f getSunDirection() {
        float angle = -getWorldTime() * 2.0f * (float)Math.PI;
        Vector3f dir = new Vector3f(
                (float)Math.cos(angle) * 0.95f,
                (float)Math.sin(angle),
                (float)Math.cos(angle) * 0.4f
        );
        return dir.normalize().negate();

    }

    public void reset() {
        lastUpdateMillis = System.currentTimeMillis();
        pausedTime = startTimeOffset;
        paused = false;
    }
}

