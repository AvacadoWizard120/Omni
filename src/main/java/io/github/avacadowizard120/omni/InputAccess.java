package io.github.avacadowizard120.omni;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InputAccess {
    private static final String[] PLAYER_INPUT_NAMES = {"input", "f_108618_", "field_71158_b", "field_3913", "cy", "f"};
    private static final String[] LEFT_IMPULSE_NAMES = {"leftImpulse", "moveStrafe", "f_108566_", "field_78902_a", "field_3907", "a"};
    private static final String[] FORWARD_IMPULSE_NAMES = {"forwardImpulse", "moveForward", "f_108567_", "field_192832_b", "field_78900_b", "field_3905", "b"};
    private static final String[] MOVE_VECTOR_NAMES = {"moveVector", "f_381292_"};
    private static final String[] VECTOR_X_NAMES = {"x", "f_82470_", "field_189982_i"};
    private static final String[] VECTOR_Y_NAMES = {"y", "f_82471_", "field_189983_j"};

    private static final Map<Class<?>, Field> PLAYER_INPUT_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> LEFT_IMPULSE_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> FORWARD_IMPULSE_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> MOVE_VECTOR_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> VECTOR_X_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> VECTOR_Y_FIELDS = new ConcurrentHashMap<>();

    private InputAccess() {
    }

    public static Object playerInput(Object player) {
        if (player == null) {
            return null;
        }

        Class<?> playerClass = player.getClass();
        Field cached = PLAYER_INPUT_FIELDS.get(playerClass);
        if (cached != null) {
            return getObject(player, cached);
        }

        Field field = findObjectField(playerClass, player, PLAYER_INPUT_NAMES);
        if (field == null) {
            return null;
        }

        Field existing = PLAYER_INPUT_FIELDS.putIfAbsent(playerClass, field);
        return getObject(player, existing != null ? existing : field);
    }

    public static float leftImpulse(Object input) {
        return getFloat(input, LEFT_IMPULSE_FIELDS, LEFT_IMPULSE_NAMES);
    }

    public static float forwardImpulse(Object input) {
        return getFloat(input, FORWARD_IMPULSE_FIELDS, FORWARD_IMPULSE_NAMES);
    }

    public static float moveVectorX(Object input) {
        Object moveVector = getObject(input, MOVE_VECTOR_FIELDS, MOVE_VECTOR_NAMES);
        return getFloat(moveVector, VECTOR_X_FIELDS, VECTOR_X_NAMES);
    }

    public static float moveVectorY(Object input) {
        Object moveVector = getObject(input, MOVE_VECTOR_FIELDS, MOVE_VECTOR_NAMES);
        return getFloat(moveVector, VECTOR_Y_FIELDS, VECTOR_Y_NAMES);
    }

    private static Object getObject(Object owner, Map<Class<?>, Field> cache, String[] names) {
        if (owner == null) {
            return null;
        }

        Field field = getField(owner.getClass(), cache, names);
        if (field == null) {
            return null;
        }

        try {
            return field.get(owner);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object getObject(Object owner, Field field) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return null;
        }
    }

    private static float getFloat(Object owner, Map<Class<?>, Field> cache, String[] names) {
        if (owner == null) {
            return 0.0F;
        }

        Field field = getField(owner.getClass(), cache, names);
        if (field == null) {
            return 0.0F;
        }

        try {
            return field.getFloat(owner);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return 0.0F;
        }
    }

    private static Field getField(Class<?> ownerClass, Map<Class<?>, Field> cache, String[] names) {
        Field cached = cache.get(ownerClass);
        if (cached != null) {
            return cached;
        }

        Field field = findField(ownerClass, names);
        if (field == null) {
            return null;
        }

        Field existing = cache.putIfAbsent(ownerClass, field);
        return existing != null ? existing : field;
    }

    private static Field findField(Class<?> ownerClass, String[] names) {
        for (Class<?> current = ownerClass; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
        }

        return null;
    }

    private static Field findObjectField(Class<?> ownerClass, Object owner, String[] names) {
        for (Class<?> current = ownerClass; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    if (isInputObject(field.get(owner))) {
                        return field;
                    }
                } catch (IllegalAccessException | NoSuchFieldException | RuntimeException ignored) {
                }
            }
        }

        return null;
    }

    private static boolean isInputObject(Object value) {
        if (value == null) {
            return false;
        }

        Class<?> valueClass = value.getClass();
        return (hasFloatField(valueClass, LEFT_IMPULSE_NAMES) && hasFloatField(valueClass, FORWARD_IMPULSE_NAMES))
                || findField(valueClass, MOVE_VECTOR_NAMES) != null;
    }

    private static boolean hasFloatField(Class<?> ownerClass, String[] names) {
        Field field = findField(ownerClass, names);
        return field != null && field.getType() == float.class;
    }
}
