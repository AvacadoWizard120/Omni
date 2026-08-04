package io.github.avacadowizard120.omni.liteloader;

import io.github.avacadowizard120.omni.OmniConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OmniLiteHooks {
    private static final OmniLiteMappings.Mapping MAPPING = OmniLiteMappings.CURRENT;
    private static final String[] INPUT_FIELDS = names(MAPPING.inputField, "movementInput", "field_71158_b");
    private static final String[] FORWARD_FIELDS = names("b", "moveForward", "field_192832_b", "field_78900_b");
    private static final String[] STRAFE_FIELDS = names("a", "moveStrafe", "field_78902_a");
    private static final String[] MOTION_X_FIELDS = names(MAPPING.motionXField, "motionX", "field_70159_w");
    private static final String[] MOTION_Z_FIELDS = names(MAPPING.motionZField, "motionZ", "field_70179_y");
    private static final String[] YAW_FIELDS = names(MAPPING.yawField, "rotationYaw", "field_70177_z");
    private static final String[] SPRINTING_METHODS = names(MAPPING.sprintingMethod, "isSprinting", "func_70051_ag");
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<String, Field>();
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<String, Method>();

    private OmniLiteHooks() {
    }

    public static boolean handleCommand(String rawCommand) {
        if (rawCommand == null) {
            return false;
        }

        String command = rawCommand.trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (!command.equals("omni") && !command.startsWith("omni ")) {
            return false;
        }

        String[] parts = command.split("\\s+");
        String action = parts.length > 1 ? parts[1].toLowerCase(java.util.Locale.ROOT) : "status";

        if ("enable".equals(action) || "enabled".equals(action) || "on".equals(action) || "true".equals(action)) {
            OmniConfig.setEnabled(true);
            System.out.println("[Omni] Enabled.");
            return true;
        }
        if ("disable".equals(action) || "disabled".equals(action) || "off".equals(action) || "false".equals(action)) {
            OmniConfig.setEnabled(false);
            System.out.println("[Omni] Disabled.");
            return true;
        }
        if ("reload".equals(action)) {
            OmniConfig.reload();
            System.out.println("[Omni] Config reloaded. Status: " + OmniConfig.statusText() + ".");
            return true;
        }
        if ("status".equals(action)) {
            System.out.println("[Omni] Status: " + OmniConfig.statusText() + ".");
            return true;
        }

        System.out.println("[Omni] Usage: /omni [enable|disable|status|reload]");
        return true;
    }

    public static boolean hasDirectionalImpulse(Object input, float threshold) {
        return hasDirectionalImpulse(input, null, threshold);
    }

    public static boolean hasDirectionalImpulse(Object input, String forwardFieldName, float threshold) {
        if (input == null) {
            return false;
        }

        MovementInputValues movement = readMovementInput(input, forwardFieldName);
        float forward = movement.forward;
        float strafe = movement.strafe;
        if (!OmniConfig.isEnabled()) {
            return forward >= threshold;
        }

        return Math.abs(forward) >= threshold || Math.abs(strafe) >= threshold;
    }

    public static float directionalSpeedMultiplier(Object entity, float side, float forward) {
        if (entity == null || !OmniConfig.isEnabled() || !isSprinting(entity)) {
            return 1.0F;
        }

        Object input = readMovementInputObject(entity);
        if (input == null) {
            return 1.0F;
        }

        float effectiveSide = side;
        float effectiveForward = forward;
        if ((double) (effectiveSide * effectiveSide + effectiveForward * effectiveForward) < 1.0E-7D) {
            MovementInputValues movement = readMovementInput(input, null);
            effectiveSide = movement.strafe;
            effectiveForward = movement.forward;
        }

        if ((double) (effectiveSide * effectiveSide + effectiveForward * effectiveForward) < 1.0E-7D) {
            return 1.0F;
        }

        float multiplier = OmniConfig.directionMultiplier(effectiveForward, effectiveSide);
        return Math.abs(multiplier - 1.0F) < 1.0E-5F ? 1.0F : multiplier;
    }

    public static void applyDirectionalSprintJump(Object entity) {
        if (entity == null) {
            return;
        }

        if (!OmniConfig.isEnabled()) {
            applyVanillaSprintJump(entity);
            return;
        }

        Object input = readMovementInputObject(entity);
        if (input == null) {
            applyVanillaSprintJump(entity);
            return;
        }

        MovementInputValues movement = readMovementInput(input, null);
        float side = movement.strafe;
        float forward = movement.forward;
        double lengthSqr = (double) side * (double) side + (double) forward * (double) forward;
        if (lengthSqr < 1.0E-7D) {
            applyVanillaSprintJump(entity);
            return;
        }

        if (lengthSqr > 1.0D) {
            double length = Math.sqrt(lengthSqr);
            side = (float) ((double) side / length);
            forward = (float) ((double) forward / length);
        }

        float yawRad = readFloat(entity, YAW_FIELDS) * ((float) Math.PI / 180.0F);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double multiplier = (double) OmniConfig.directionMultiplier(forward, side);
        double boostX = ((double) side * cos - (double) forward * sin) * 0.2D * multiplier;
        double boostZ = ((double) forward * cos + (double) side * sin) * 0.2D * multiplier;

        writeDouble(entity, MOTION_X_FIELDS, readDouble(entity, MOTION_X_FIELDS) + boostX);
        writeDouble(entity, MOTION_Z_FIELDS, readDouble(entity, MOTION_Z_FIELDS) + boostZ);
    }

    private static void applyVanillaSprintJump(Object entity) {
        float yawRad = readFloat(entity, YAW_FIELDS) * ((float) Math.PI / 180.0F);
        double boostX = (double) (-Math.sin(yawRad) * 0.2F);
        double boostZ = (double) (Math.cos(yawRad) * 0.2F);
        writeDouble(entity, MOTION_X_FIELDS, readDouble(entity, MOTION_X_FIELDS) + boostX);
        writeDouble(entity, MOTION_Z_FIELDS, readDouble(entity, MOTION_Z_FIELDS) + boostZ);
    }

    private static Object readObject(Object target, String[] names) {
        Field field = findField(target.getClass(), names, Object.class);
        if (field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            return null;
        }
    }

    private static Object readMovementInputObject(Object entity) {
        Object known = readObject(entity, INPUT_FIELDS);
        if (known != null && looksLikeMovementInput(known.getClass())) {
            return known;
        }

        for (Class<?> cursor = entity.getClass(); cursor != null; cursor = cursor.getSuperclass()) {
            Field[] fields = cursor.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value != null && looksLikeMovementInput(value.getClass())) {
                        return value;
                    }
                } catch (IllegalAccessException | RuntimeException ignored) {
                    // Try the next candidate field.
                }
            }
        }

        return null;
    }

    private static boolean isSprinting(Object target) {
        Method method = findMethod(target.getClass(), SPRINTING_METHODS);
        if (method == null) {
            return false;
        }

        try {
            return ((Boolean) method.invoke(target)).booleanValue();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return false;
        }
    }

    private static Method findMethod(Class<?> type, String[] names) {
        String key = type.getName() + ":" + firstUsableName(names);
        Method cached = METHODS.get(key);
        if (cached != null) {
            return cached;
        }

        Method found = searchMethod(type, names);
        if (found != null) {
            METHODS.put(key, found);
        }
        return found;
    }

    private static Method searchMethod(Class<?> type, String[] names) {
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            for (String name : names) {
                try {
                    Method method = cursor.getDeclaredMethod(name);
                    if (method.getReturnType() != Boolean.TYPE) {
                        continue;
                    }
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ex) {
                    // Try the next runtime name.
                }
            }
        }
        return null;
    }

    private static MovementInputValues readMovementInput(Object input, String forwardFieldName) {
        String[] forwardNames = forwardFieldName == null
                ? FORWARD_FIELDS
                : prepend(forwardFieldName, FORWARD_FIELDS);
        Field forwardField = findField(input.getClass(), forwardNames, Float.TYPE);
        Field strafeField = findField(input.getClass(), STRAFE_FIELDS, Float.TYPE);

        if (forwardField != null && (strafeField == null || sameField(forwardField, strafeField))) {
            strafeField = findOtherFloatField(input.getClass(), forwardField);
        }

        if (forwardField == null || strafeField == null) {
            Field[] movementFields = findMovementFloatFields(input.getClass());
            if (movementFields.length >= 2) {
                if (strafeField == null) {
                    strafeField = movementFields[0];
                }
                if (forwardField == null) {
                    forwardField = sameField(strafeField, movementFields[1]) ? movementFields[0] : movementFields[1];
                }
            }
        }

        return new MovementInputValues(readFloat(input, forwardField), readFloat(input, strafeField));
    }

    private static float readFloat(Object target, String[] names) {
        Field field = findField(target.getClass(), names, Float.TYPE);
        return readFloat(target, field);
    }

    private static float readFloat(Object target, Field field) {
        if (field == null) {
            return 0.0F;
        }
        try {
            return field.getFloat(target);
        } catch (IllegalAccessException ex) {
            return 0.0F;
        }
    }

    private static double readDouble(Object target, String[] names) {
        Field field = findField(target.getClass(), names, Double.TYPE);
        if (field == null) {
            return 0.0D;
        }
        try {
            return field.getDouble(target);
        } catch (IllegalAccessException ex) {
            return 0.0D;
        }
    }

    private static void writeDouble(Object target, String[] names, double value) {
        Field field = findField(target.getClass(), names, Double.TYPE);
        if (field == null) {
            return;
        }
        try {
            field.setDouble(target, value);
        } catch (IllegalAccessException ex) {
            // Leave vanilla motion untouched if the runtime denies reflective access.
        }
    }

    private static Field findField(Class<?> type, String[] names, Class<?> expectedType) {
        String key = type.getName() + ":" + expectedType.getName() + ":" + firstUsableName(names);
        Field cached = FIELDS.get(key);
        if (cached != null) {
            return cached;
        }

        Field found = searchField(type, names, expectedType);
        if (found != null) {
            FIELDS.put(key, found);
        }
        return found;
    }

    private static String firstUsableName(String[] names) {
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                if (names[i] != null && !names[i].isEmpty()) {
                    return names[i];
                }
            }
        }
        return "<unnamed>";
    }

    private static Field searchField(Class<?> type, String[] names, Class<?> expectedType) {
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = cursor.getDeclaredField(name);
                    if (expectedType == Object.class ? field.getType().isPrimitive() : field.getType() != expectedType) {
                        continue;
                    }
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ex) {
                    // Try the next runtime name.
                }
            }
        }
        return null;
    }

    private static String[] prepend(String first, String[] rest) {
        String[] names = new String[rest.length + 1];
        names[0] = first;
        System.arraycopy(rest, 0, names, 1, rest.length);
        return names;
    }

    private static String[] names(String... values) {
        int count = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && !values[i].isEmpty()) {
                count++;
            }
        }

        String[] names = new String[count];
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && !values[i].isEmpty()) {
                names[index++] = values[i];
            }
        }
        return names;
    }

    private static boolean looksLikeMovementInput(Class<?> type) {
        return findMovementFloatFields(type).length >= 2;
    }

    private static Field findOtherFloatField(Class<?> type, Field excluded) {
        Field[] fields = findMovementFloatFields(type);
        for (int i = 0; i < fields.length; i++) {
            if (!sameField(fields[i], excluded)) {
                return fields[i];
            }
        }
        return null;
    }

    private static Field[] findMovementFloatFields(Class<?> type) {
        Field[] found = new Field[2];
        int count = 0;
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            Field[] fields = cursor.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if (Modifier.isStatic(field.getModifiers()) || field.getType() != Float.TYPE) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (count < found.length) {
                    found[count] = field;
                }
                count++;
                if (count >= 2) {
                    return found;
                }
            }
        }
        if (count == found.length) {
            return found;
        }

        Field[] exact = new Field[count];
        System.arraycopy(found, 0, exact, 0, count);
        return exact;
    }

    private static boolean sameField(Field left, Field right) {
        return left != null && right != null
                && left.getDeclaringClass() == right.getDeclaringClass()
                && left.getName().equals(right.getName());
    }

    private static final class MovementInputValues {
        private final float forward;
        private final float strafe;

        private MovementInputValues(float forward, float strafe) {
            this.forward = forward;
            this.strafe = strafe;
        }
    }
}
