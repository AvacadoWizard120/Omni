package io.github.avacadowizard120.omni.backport;

public enum OmniBackportDirection {
    FORWARD("forward"),
    BACKWARD("backward"),
    LEFT("left"),
    RIGHT("right"),
    FORWARD_LEFT("forward_left"),
    FORWARD_RIGHT("forward_right"),
    BACKWARD_LEFT("backward_left"),
    BACKWARD_RIGHT("backward_right");

    private static final float EPSILON = 1.0E-5F;

    private final String configKey;

    OmniBackportDirection(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return this.configKey;
    }

    public static OmniBackportDirection fromInput(float forward, float side) {
        boolean movingForward = forward > EPSILON;
        boolean movingBackward = forward < -EPSILON;
        boolean movingLeft = side > EPSILON;
        boolean movingRight = side < -EPSILON;

        if (movingForward && movingLeft) {
            return FORWARD_LEFT;
        }
        if (movingForward && movingRight) {
            return FORWARD_RIGHT;
        }
        if (movingBackward && movingLeft) {
            return BACKWARD_LEFT;
        }
        if (movingBackward && movingRight) {
            return BACKWARD_RIGHT;
        }
        if (movingBackward) {
            return BACKWARD;
        }
        if (movingLeft) {
            return LEFT;
        }
        if (movingRight) {
            return RIGHT;
        }
        return FORWARD;
    }
}
