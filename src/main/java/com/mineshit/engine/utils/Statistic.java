package com.mineshit.engine.utils;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

public class Statistic {
    private static final Map<String, Object> stats = new HashMap<>();

    public static void set(String key, Object value) {
        stats.put(key, value);
    }

    public static <T> T get(String key, Class<T> type) {
        Object value = stats.get(key);
        if (value == null) return null;
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Invalid type for key " + key + ": expected " + type.getSimpleName());
        }
        return type.cast(value);
    }

    public static void increment(String key) {
        Object value = stats.getOrDefault(key, 0L);
        if (value instanceof Long l) {
            stats.put(key, l + 1);
        } else {
            throw new IllegalArgumentException("Cannot increment non-long stat: " + key);
        }
    }

    public static void add(String key, int val) {
        Object value = stats.getOrDefault(key, 0L);
        if (value instanceof Long l) {
            stats.put(key, l + val);
        } else {
            throw new IllegalArgumentException("Cannot add non-long stat: " + key);
        }
    }

    public static void reset(String key) {
        stats.remove(key);
    }

    public static void resetAll() {
        stats.clear();
    }

    public static Map<String, Object> getAll() {
        return Collections.unmodifiableMap(stats);
    }
}


