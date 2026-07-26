package io.github.avacadowizard120.omni.backport;

public final class OmniBackportMath {
    private OmniBackportMath() {
    }

    public static boolean hasDirectionalImpulse(float forward, float side, float threshold) {
        return Math.abs(forward) >= threshold || Math.abs(side) >= threshold;
    }

    public static double[] sprintJumpBoost(float forward, float side, float yawDegrees, double vanillaBoost, double multiplier) {
        double localX = side;
        double localZ = forward;
        double localLengthSqr = localX * localX + localZ * localZ;

        if (localLengthSqr > 1.0D) {
            double localLength = Math.sqrt(localLengthSqr);
            localX /= localLength;
            localZ /= localLength;
        }

        double yawRad = yawDegrees * Math.PI / 180.0D;
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double worldX = localX * cos - localZ * sin;
        double worldZ = localZ * cos + localX * sin;
        double worldLengthSqr = worldX * worldX + worldZ * worldZ;

        if (worldLengthSqr < 1.0E-7D) {
            return new double[] {0.0D, 0.0D};
        }

        double worldLength = Math.sqrt(worldLengthSqr);
        double scale = vanillaBoost * multiplier / worldLength;
        return new double[] {worldX * scale, worldZ * scale};
    }
}
