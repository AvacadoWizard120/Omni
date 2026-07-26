package io.github.avacadowizard120.omni;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class OmniForge114Hooks {
    private static final Map<Object, VelocitySnapshot> PRE_JUMP = Collections.synchronizedMap(new WeakHashMap<Object, VelocitySnapshot>());

    private OmniForge114Hooks() {
    }

    public static boolean hasForwardImpulse(Object input) {
        float forward = InputAccess.forwardImpulse(input);
        if (!OmniConfig.isEnabled()) {
            return forward > 1.0E-5F;
        }
        return Math.abs(forward) > 1.0E-5F || Math.abs(InputAccess.leftImpulse(input)) > 1.0E-5F;
    }

    public static boolean hasEnoughImpulseToStartSprinting(Object player) {
        Object input = InputAccess.playerInput(player);
        if (input == null) {
            return false;
        }

        float forward = InputAccess.forwardImpulse(input);
        float side = InputAccess.leftImpulse(input);
        boolean underwater = MovementAccess.isUnderWater(player);
        if (!OmniConfig.isEnabled()) {
            return underwater ? forward > 1.0E-5F : forward >= 0.8F;
        }
        if (underwater) {
            return Math.abs(forward) > 1.0E-5F || Math.abs(side) > 1.0E-5F;
        }
        return Math.abs(forward) >= 0.8F || Math.abs(side) >= 0.8F;
    }

    public static float directionalSpeedMultiplier(Object entity) {
        if (!OmniConfig.isEnabled() || !OmniConfig.hasCustomSpeed() || !MovementAccess.isSprinting(entity)) {
            return 1.0F;
        }

        Object input = InputAccess.playerInput(entity);
        if (input == null) {
            return 1.0F;
        }

        float side = InputAccess.leftImpulse(input);
        float forward = InputAccess.forwardImpulse(input);
        if ((double) (side * side + forward * forward) < 1.0E-7D) {
            return 1.0F;
        }

        float multiplier = OmniConfig.directionMultiplier(forward, side);
        return Math.abs(multiplier - 1.0F) < 1.0E-5F ? 1.0F : multiplier;
    }

    public static void capturePreJumpVelocity(Object entity) {
        if (InputAccess.playerInput(entity) != null) {
            PRE_JUMP.put(entity, VelocitySnapshot.of(entity));
        }
    }

    public static void redirectSprintJumpBoost(Object entity) {
        VelocitySnapshot before = PRE_JUMP.remove(entity);
        if (before == null || !OmniConfig.isEnabled() || !MovementAccess.isSprinting(entity)) {
            return;
        }

        Object input = InputAccess.playerInput(entity);
        if (input == null) {
            return;
        }

        float side = InputAccess.leftImpulse(input);
        float forward = InputAccess.forwardImpulse(input);
        if ((double) (side * side + forward * forward) < 1.0E-7D) {
            return;
        }

        double afterX = MovementAccess.deltaX(entity);
        double afterY = MovementAccess.deltaY(entity);
        double afterZ = MovementAccess.deltaZ(entity);
        double vanillaAddedX = afterX - before.x;
        double vanillaAddedZ = afterZ - before.z;
        double vanillaBoostLen = Math.sqrt(vanillaAddedX * vanillaAddedX + vanillaAddedZ * vanillaAddedZ);
        if (vanillaBoostLen < 1.0E-7D) {
            return;
        }

        Direction direction = Direction.fromInput(entity, forward, side);
        if (direction == null) {
            return;
        }

        MovementAccess.setDeltaMovement(
                entity,
                before.x + direction.x * vanillaBoostLen,
                afterY,
                before.z + direction.z * vanillaBoostLen
        );
    }

    private static final class Direction {
        private final double x;
        private final double z;

        private Direction(double x, double z) {
            this.x = x;
            this.z = z;
        }

        private static Direction fromInput(Object entity, float forward, float side) {
            double localX = side;
            double localZ = forward;
            double localLenSqr = localX * localX + localZ * localZ;
            if (localLenSqr < 1.0E-7D) {
                return null;
            }
            if (localLenSqr > 1.0D) {
                double localLen = Math.sqrt(localLenSqr);
                localX /= localLen;
                localZ /= localLen;
            }

            float yawRad = MovementAccess.yRot(entity) * ((float) Math.PI / 180.0F);
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            double worldX = localX * cos - localZ * sin;
            double worldZ = localZ * cos + localX * sin;
            double worldLenSqr = worldX * worldX + worldZ * worldZ;
            if (worldLenSqr < 1.0E-7D) {
                return null;
            }

            double worldLen = Math.sqrt(worldLenSqr);
            return new Direction(worldX / worldLen, worldZ / worldLen);
        }
    }

    private static final class VelocitySnapshot {
        private final double x;
        private final double z;

        private VelocitySnapshot(double x, double z) {
            this.x = x;
            this.z = z;
        }

        private static VelocitySnapshot of(Object entity) {
            return new VelocitySnapshot(MovementAccess.deltaX(entity), MovementAccess.deltaZ(entity));
        }
    }
}
