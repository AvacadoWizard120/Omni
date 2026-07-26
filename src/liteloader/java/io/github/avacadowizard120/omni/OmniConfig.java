package io.github.avacadowizard120.omni;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class OmniConfig {
    private static final float MIN_MULTIPLIER = 0.0F;

    private static final Object LOCK = new Object();
    private static final File CONFIG_FILE = new File(new File(System.getProperty("user.dir"), "config"), "omni.properties");

    private static volatile boolean loaded;
    private static volatile long lastModified = -1L;
    private static volatile long nextRefreshAt;

    private static volatile boolean enabled = true;
    private static volatile float forwardMultiplier = 1.0F;
    private static volatile float backwardMultiplier = 1.0F;
    private static volatile float leftMultiplier = 1.0F;
    private static volatile float rightMultiplier = 1.0F;
    private static volatile float forwardLeftMultiplier = 1.0F;
    private static volatile float forwardRightMultiplier = 1.0F;
    private static volatile float backwardLeftMultiplier = 1.0F;
    private static volatile float backwardRightMultiplier = 1.0F;

    private OmniConfig() {
    }

    public static boolean isEnabled() {
        refreshIfNeeded(false);
        return enabled;
    }

    public static void ensureLoaded() {
        refreshIfNeeded(false);
    }

    public static boolean hasCustomSpeed() {
        refreshIfNeeded(false);
        return Math.abs(forwardMultiplier - 1.0F) >= 1.0E-5F
                || Math.abs(backwardMultiplier - 1.0F) >= 1.0E-5F
                || Math.abs(leftMultiplier - 1.0F) >= 1.0E-5F
                || Math.abs(rightMultiplier - 1.0F) >= 1.0E-5F
                || Math.abs(forwardLeftMultiplier - 1.0F) >= 1.0E-5F
                || Math.abs(forwardRightMultiplier - 1.0F) >= 1.0E-5F
                || Math.abs(backwardLeftMultiplier - 1.0F) >= 1.0E-5F
                || Math.abs(backwardRightMultiplier - 1.0F) >= 1.0E-5F;
    }

    public static float directionMultiplier(float forward, float side) {
        refreshIfNeeded(false);

        boolean movingForward = forward > 1.0E-5F;
        boolean movingBackward = forward < -1.0E-5F;
        boolean movingLeft = side > 1.0E-5F;
        boolean movingRight = side < -1.0E-5F;

        if (movingForward && movingLeft) {
            return forwardLeftMultiplier;
        }
        if (movingForward && movingRight) {
            return forwardRightMultiplier;
        }
        if (movingBackward && movingLeft) {
            return backwardLeftMultiplier;
        }
        if (movingBackward && movingRight) {
            return backwardRightMultiplier;
        }
        if (movingBackward) {
            return backwardMultiplier;
        }
        if (movingLeft) {
            return leftMultiplier;
        }
        if (movingRight) {
            return rightMultiplier;
        }
        return forwardMultiplier;
    }

    public static void setEnabled(boolean value) {
        refreshIfNeeded(false);
        synchronized (LOCK) {
            enabled = value;
            write();
            lastModified = CONFIG_FILE.lastModified();
            nextRefreshAt = System.currentTimeMillis() + 1000L;
        }
    }

    public static void reload() {
        refreshIfNeeded(true);
    }

    public static String statusText() {
        refreshIfNeeded(false);
        return enabled ? "enabled" : "disabled";
    }

    private static void refreshIfNeeded(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && loaded && now < nextRefreshAt) {
            return;
        }

        synchronized (LOCK) {
            if (!force && loaded && now < nextRefreshAt) {
                return;
            }

            nextRefreshAt = now + 1000L;
            if (!CONFIG_FILE.exists()) {
                write();
                loaded = true;
                lastModified = CONFIG_FILE.lastModified();
                return;
            }

            long modified = CONFIG_FILE.lastModified();
            if (!force && loaded && modified == lastModified) {
                return;
            }

            read();
            loaded = true;
            lastModified = modified;
        }
    }

    private static void read() {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(CONFIG_FILE), StandardCharsets.UTF_8))) {
            properties.load(reader);
        } catch (IOException ignored) {
            write();
            return;
        }

        enabled = readBoolean(properties, "enabled", true);
        forwardMultiplier = readMultiplier(properties, "forward_multiplier", 1.0F);
        backwardMultiplier = readMultiplier(properties, "backward_multiplier", 1.0F);
        float oldSideways = readMultiplier(properties, "sideways_multiplier", 1.0F);
        float oldDiagonal = readMultiplier(properties, "diagonal_multiplier", 1.0F);
        leftMultiplier = readMultiplier(properties, "left_multiplier", oldSideways);
        rightMultiplier = readMultiplier(properties, "right_multiplier", oldSideways);
        forwardLeftMultiplier = readMultiplier(properties, "forward_left_multiplier", oldDiagonal);
        forwardRightMultiplier = readMultiplier(properties, "forward_right_multiplier", oldDiagonal);
        backwardLeftMultiplier = readMultiplier(properties, "backward_left_multiplier", oldDiagonal);
        backwardRightMultiplier = readMultiplier(properties, "backward_right_multiplier", oldDiagonal);
    }

    private static boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized) || "on".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized) || "off".equalsIgnoreCase(normalized)) {
            return false;
        }
        return fallback;
    }

    private static float readMultiplier(Properties properties, String key, float fallback) {
        String value = properties.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return sanitizeMultiplier(Float.parseFloat(value.trim()), fallback);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void write() {
        File parent = CONFIG_FILE.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), StandardCharsets.UTF_8))) {
            writer.write("# Omni config");
            writer.newLine();
            writer.write("# Set enabled=false to disable Omni.");
            writer.newLine();
            writer.write("enabled=" + enabled);
            writer.newLine();
            writer.newLine();
            writer.write("# Sprint-speed multipliers: 1.0 = vanilla, 0.5 = half, 2.0 = double.");
            writer.newLine();
            writer.write("# No upper limit; high values may be hard to control.");
            writer.newLine();
            writer.write("forward_multiplier=" + format(forwardMultiplier));
            writer.newLine();
            writer.write("backward_multiplier=" + format(backwardMultiplier));
            writer.newLine();
            writer.write("left_multiplier=" + format(leftMultiplier));
            writer.newLine();
            writer.write("right_multiplier=" + format(rightMultiplier));
            writer.newLine();
            writer.write("forward_left_multiplier=" + format(forwardLeftMultiplier));
            writer.newLine();
            writer.write("forward_right_multiplier=" + format(forwardRightMultiplier));
            writer.newLine();
            writer.write("backward_left_multiplier=" + format(backwardLeftMultiplier));
            writer.newLine();
            writer.write("backward_right_multiplier=" + format(backwardRightMultiplier));
            writer.newLine();
        } catch (IOException ignored) {
        }
    }

    private static float sanitizeMultiplier(float value, float fallback) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return fallback;
        }
        if (value < MIN_MULTIPLIER) {
            return MIN_MULTIPLIER;
        }
        return value;
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 1.0E-5F) {
            return Integer.toString(Math.round(value)) + ".0";
        }
        return Float.toString(value);
    }
}
