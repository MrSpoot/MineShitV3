package com.mineshit.engine.core;

public class Timer {

    private final double interval;
    private double lastTime;
    private double accumulator;

    public Timer(int frequency) {
        this.interval = 1.0 / frequency;
        this.lastTime = getTime();
        this.accumulator = 0;
    }

    public void update() {
        double now = getTime();
        double deltaTime = now - lastTime;
        lastTime = now;
        accumulator += deltaTime;
    }

    public boolean shouldTick() {
        if (accumulator >= interval) {
            accumulator -= interval;
            return true;
        }
        return false;
    }

    public float getFixedStep() {
        return (float) interval;
    }


    public float getAlpha() {
        return Math.min(1f, (float)(accumulator / interval));

    }

    private double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }
}
