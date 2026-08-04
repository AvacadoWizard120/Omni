package io.github.avacadowizard120.omni.liteloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

final class OmniLiteMappings {
    static final Mapping CURRENT = load();

    private OmniLiteMappings() {
    }

    private static Mapping load() {
        Properties properties = new Properties();
        InputStream stream = OmniLiteMappings.class.getResourceAsStream("/assets/omni/liteloader.properties");
        if (stream == null) {
            return Mapping.DEOBFUSCATED;
        }

        try {
            properties.load(stream);
            return forVersion(properties.getProperty("minecraftVersion", "").trim());
        } catch (IOException ex) {
            return Mapping.DEOBFUSCATED;
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // Nothing useful can be done if a classpath resource fails to close.
            }
        }
    }

    private static Mapping forVersion(String version) {
        if ("1.6.4".equals(version)) {
            return new Mapping("bex", "bdi", "of", "c", "c", "b", "be", "ai", "x", "z", "A");
        }
        if ("1.7.2".equals(version)) {
            return new Mapping("blc", "bje", "rh", "c", "e", "a", "bj", "ao", "w", "y", "z");
        }
        if ("1.7.10".equals(version)) {
            return new Mapping("blk", "bjk", "sv", "c", "e", "a", "bj", "ao", "v", "x", "y");
        }
        if ("1.8".equals(version)) {
            return new Mapping("cio", "cio", "xm", "b", "m", "e", "bE", "ax", "v", "x", "y");
        }
        if ("1.8.9".equals(version)) {
            return new Mapping("bew", "bew", "pr", "b", "m", "e", "bF", "aw", "v", "x", "y");
        }
        if ("1.9".equals(version)) {
            return new Mapping("bmt", "bmt", "sa", "e", "n", "g", "ch", "aL", "s", "u", "v");
        }
        if ("1.9.4".equals(version)) {
            return new Mapping("bmr", "bmr", "sa", "e", "n", "g", "ci", "aL", "s", "u", "v");
        }
        if ("1.10".equals(version) || "1.10.2".equals(version)) {
            return new Mapping("bnn", "bnn", "sf", "e", "n", "g", "cl", "aN", "s", "u", "v");
        }
        if ("1.11".equals(version)) {
            return new Mapping("bpq", "bpq", "sv", "e", "n", "g", "cm", "aN", "s", "u", "v");
        }
        if ("1.11.2".equals(version)) {
            return new Mapping("bps", "bps", "sw", "e", "n", "g", "cm", "aN", "s", "u", "v");
        }
        if ("1.12".equals(version)) {
            return new Mapping("bub", "bub", "vn", "e", "n", "g", "cu", "aV", "s", "u", "v");
        }
        if ("1.12.1".equals(version) || "1.12.2".equals(version)) {
            return new Mapping("bud", "bud", "vp", "e", "n", "g", "cu", "aV", "s", "u", "v");
        }
        return Mapping.DEOBFUSCATED;
    }

    static final class Mapping {
        static final Mapping DEOBFUSCATED = new Mapping(null, null, null, null, null, null, null, null, null, null, null);

        final String playerClass;
        final String chatClass;
        final String livingClass;
        final String inputField;
        final String livingUpdateMethod;
        final String chatMethod;
        final String jumpMethod;
        final String sprintingMethod;
        final String motionXField;
        final String motionZField;
        final String yawField;

        Mapping(
                String playerClass,
                String chatClass,
                String livingClass,
                String inputField,
                String livingUpdateMethod,
                String chatMethod,
                String jumpMethod,
                String sprintingMethod,
                String motionXField,
                String motionZField,
                String yawField
        ) {
            this.playerClass = playerClass;
            this.chatClass = chatClass;
            this.livingClass = livingClass;
            this.inputField = inputField;
            this.livingUpdateMethod = livingUpdateMethod;
            this.chatMethod = chatMethod;
            this.jumpMethod = jumpMethod;
            this.sprintingMethod = sprintingMethod;
            this.motionXField = motionXField;
            this.motionZField = motionZField;
            this.yawField = yawField;
        }
    }
}
