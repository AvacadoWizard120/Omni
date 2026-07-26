package io.github.avacadowizard120.omni;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MovementAccess {
    private static final String[] GET_DELTA_MOVEMENT_NAMES = {"getDeltaMovement", "m_20184_", "func_213322_ci", "method_18798", "cj", "cs"};
    private static final String[] SET_DELTA_MOVEMENT_NAMES = {"setDeltaMovement", "m_20334_", "func_213293_j", "method_18800", "k", "m"};
    private static final String[] GET_Y_ROT_NAMES = {"getYRot", "m_146908_", "method_19330"};
    private static final String[] Y_ROT_FIELD_NAMES = {"yRot", "f_19857_", "field_70177_z", "field_6031", "s"};
    private static final String[] IS_SPRINTING_NAMES = {"isSprinting", "m_20142_", "func_70051_ag", "method_5624", "bi", "bp"};
    private static final String[] IS_UNDER_WATER_NAMES = {"isUnderWater", "isInWater", "m_5842_", "func_205012_q", "func_70090_H"};
    private static final String[] VECTOR_X_NAMES = {"x", "f_82479_", "field_72450_a", "field_1352", "b"};
    private static final String[] VECTOR_Y_NAMES = {"y", "f_82480_", "field_72448_b", "field_1351", "c"};
    private static final String[] VECTOR_Z_NAMES = {"z", "f_82481_", "field_72449_c", "field_1350", "d"};

    private static final Map<Class<?>, Method> GET_DELTA_MOVEMENT_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> SET_DELTA_MOVEMENT_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> GET_Y_ROT_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> Y_ROT_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> IS_SPRINTING_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> IS_UNDER_WATER_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> VECTOR_X_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> VECTOR_Y_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> VECTOR_Z_FIELDS = new ConcurrentHashMap<>();

    private MovementAccess() {
    }

    public static double deltaX(Object entity) {
        return vectorX(deltaMovement(entity));
    }

    public static double deltaY(Object entity) {
        return vectorY(deltaMovement(entity));
    }

    public static double deltaZ(Object entity) {
        return vectorZ(deltaMovement(entity));
    }

    public static void setDeltaMovement(Object entity, double x, double y, double z) {
        Method method = getMethod(
                entity,
                SET_DELTA_MOVEMENT_METHODS,
                SET_DELTA_MOVEMENT_NAMES,
                double.class,
                double.class,
                double.class
        );
        if (method == null) {
            return;
        }

        try {
            method.invoke(entity, x, y, z);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
        }
    }

    public static float yRot(Object entity) {
        Method method = getMethod(entity, GET_Y_ROT_METHODS, GET_Y_ROT_NAMES);
        if (method == null) {
            return getFloat(entity, Y_ROT_FIELDS, Y_ROT_FIELD_NAMES);
        }

        try {
            Object value = method.invoke(entity);
            return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            return 0.0F;
        }
    }

    public static boolean isSprinting(Object entity) {
        return getBoolean(entity, IS_SPRINTING_METHODS, IS_SPRINTING_NAMES);
    }

    public static boolean isUnderWater(Object entity) {
        return getBoolean(entity, IS_UNDER_WATER_METHODS, IS_UNDER_WATER_NAMES);
    }

    private static Object deltaMovement(Object entity) {
        Method method = getMethod(entity, GET_DELTA_MOVEMENT_METHODS, GET_DELTA_MOVEMENT_NAMES);
        if (method == null) {
            return null;
        }

        try {
            return method.invoke(entity);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            return null;
        }
    }

    private static double vectorX(Object vector) {
        return getDouble(vector, VECTOR_X_FIELDS, VECTOR_X_NAMES);
    }

    private static double vectorY(Object vector) {
        return getDouble(vector, VECTOR_Y_FIELDS, VECTOR_Y_NAMES);
    }

    private static double vectorZ(Object vector) {
        return getDouble(vector, VECTOR_Z_FIELDS, VECTOR_Z_NAMES);
    }

    private static boolean getBoolean(Object owner, Map<Class<?>, Method> cache, String[] names) {
        Method method = getMethod(owner, cache, names);
        if (method == null) {
            return false;
        }

        try {
            Object value = method.invoke(owner);
            return value instanceof Boolean && (Boolean) value;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            return false;
        }
    }

    private static double getDouble(Object owner, Map<Class<?>, Field> cache, String[] names) {
        if (owner == null) {
            return 0.0D;
        }

        Field field = getField(owner.getClass(), cache, names);
        if (field == null) {
            return 0.0D;
        }

        try {
            return field.getDouble(owner);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return 0.0D;
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

    private static Method getMethod(Object owner, Map<Class<?>, Method> cache, String[] names, Class<?>... parameters) {
        if (owner == null) {
            return null;
        }

        Class<?> ownerClass = owner.getClass();
        Method cached = cache.get(ownerClass);
        if (cached != null) {
            return cached;
        }

        Method method = findMethod(ownerClass, names, parameters);
        if (method == null) {
            return null;
        }

        Method existing = cache.putIfAbsent(ownerClass, method);
        return existing != null ? existing : method;
    }

    private static Method findMethod(Class<?> ownerClass, String[] names, Class<?>... parameters) {
        for (Class<?> current = ownerClass; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    Method method = current.getDeclaredMethod(name, parameters);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                }
            }
        }

        return null;
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
}
