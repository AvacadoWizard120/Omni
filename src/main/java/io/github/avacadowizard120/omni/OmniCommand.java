package io.github.avacadowizard120.omni;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

public final class OmniCommand {
    private OmniCommand() {
    }

    public static boolean handle(String rawCommand) {
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
        String action = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "status";

        if ("enable".equals(action) || "enabled".equals(action) || "on".equals(action) || "true".equals(action)) {
            OmniConfig.setEnabled(true);
            feedback("Enabled.");
            return true;
        }

        if ("disable".equals(action) || "disabled".equals(action) || "off".equals(action) || "false".equals(action)) {
            OmniConfig.setEnabled(false);
            feedback("Disabled.");
            return true;
        }

        if ("reload".equals(action)) {
            OmniConfig.reload();
            feedback("Config reloaded. Status: " + OmniConfig.statusText() + ".");
            return true;
        }

        if ("status".equals(action)) {
            feedback("Status: " + OmniConfig.statusText() + ".");
            return true;
        }

        feedback("Usage: /omni [enable|disable|status|reload]");
        return true;
    }

    private static void feedback(String message) {
        String formatted = "[Omni] " + message;
        if (!sendInGameMessage(formatted)) {
            System.out.println(formatted);
        }
    }

    private static boolean sendInGameMessage(String message) {
        Object player = minecraftPlayer();
        Object component = component(message);
        if (player == null || component == null) {
            return false;
        }

        Class<?> componentClass = component.getClass();
        return invoke(player, "displayClientMessage", componentClass, boolean.class, component, Boolean.FALSE)
                || invoke(player, "sendSystemMessage", componentClass, component)
                || invoke(player, "sendMessage", componentClass, UUID.class, component, new UUID(0L, 0L))
                || invoke(player, "sendMessage", componentClass, component);
    }

    private static Object minecraftPlayer() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Method getInstance = minecraftClass.getDeclaredMethod("getInstance");
            getInstance.setAccessible(true);
            Object minecraft = getInstance.invoke(null);
            Field player = findField(minecraftClass, "player");
            return player == null ? null : player.get(minecraft);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static Object component(String message) {
        Object literal = invokeStatic("net.minecraft.network.chat.Component", "literal", String.class, message);
        if (literal != null) {
            return literal;
        }

        Object text = construct("net.minecraft.network.chat.TextComponent", String.class, message);
        if (text != null) {
            return text;
        }

        text = construct("net.minecraft.util.text.StringTextComponent", String.class, message);
        if (text != null) {
            return text;
        }

        return invokeStatic("net.minecraft.text.Text", "literal", String.class, message);
    }

    private static Object invokeStatic(String className, String methodName, Class<?> parameter, Object value) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getDeclaredMethod(methodName, parameter);
            method.setAccessible(true);
            return method.invoke(null, value);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static Object construct(String className, Class<?> parameter, Object value) {
        try {
            Class<?> type = Class.forName(className);
            Constructor<?> constructor = type.getDeclaredConstructor(parameter);
            constructor.setAccessible(true);
            return constructor.newInstance(value);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    private static boolean invoke(Object target, String methodName, Class<?> parameter, Object value) {
        try {
            Method method = findMethod(target.getClass(), methodName, parameter);
            if (method == null) {
                return false;
            }
            method.invoke(target, value);
            return true;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    private static boolean invoke(Object target, String methodName, Class<?> firstParameter, Class<?> secondParameter, Object firstValue, Object secondValue) {
        try {
            Method method = findMethod(target.getClass(), methodName, firstParameter, secondParameter);
            if (method == null) {
                return false;
            }
            method.invoke(target, firstValue, secondValue);
            return true;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameters) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (name.equals(method.getName()) && matches(method.getParameterTypes(), parameters)) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean matches(Class<?>[] actual, Class<?>[] expected) {
        if (actual.length != expected.length) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (!actual[i].isAssignableFrom(expected[i])) {
                return false;
            }
        }
        return true;
    }

    private static Field findField(Class<?> owner, String name) {
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
