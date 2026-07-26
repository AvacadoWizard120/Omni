package io.github.avacadowizard120.omni;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ForgeCommandBridge {
    private static final Object LOCK = new Object();
    private static boolean registered;

    private ForgeCommandBridge() {
    }

    public static void register() {
        synchronized (LOCK) {
            if (registered) {
                return;
            }
            registered = true;
        }

        if (registerCancellableBus()) {
            return;
        }
        registerEventBus();
    }

    private static boolean registerCancellableBus() {
        try {
            Class<?> eventClass = Class.forName("net.minecraftforge.client.event.ClientChatEvent");
            Field busField = eventClass.getField("BUS");
            Object bus = busField.get(null);
            if (bus == null) {
                return false;
            }

            Method addListener = findMethod(bus.getClass(), "addListener", Predicate.class);
            if (addListener == null) {
                return false;
            }

            Predicate<Object> listener = ForgeCommandBridge::handleCancellableChatEvent;
            addListener.invoke(bus, listener);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    private static boolean registerEventBus() {
        try {
            Class<?> eventClass = Class.forName("net.minecraftforge.client.event.ClientChatEvent");
            Class<?> forgeClass = Class.forName("net.minecraftforge.common.MinecraftForge");
            Field eventBusField = forgeClass.getField("EVENT_BUS");
            Object eventBus = eventBusField.get(null);
            if (eventBus == null) {
                return false;
            }

            Class<?> priorityClass = Class.forName("net.minecraftforge.eventbus.api.EventPriority");
            Object normalPriority = Enum.valueOf(priorityClass.asSubclass(Enum.class), "NORMAL");
            Method addListener = findMethod(eventBus.getClass(), "addListener", priorityClass, boolean.class, Class.class, Consumer.class);
            if (addListener == null) {
                return false;
            }

            Consumer<Object> listener = ForgeCommandBridge::handleChatEvent;
            addListener.invoke(eventBus, normalPriority, Boolean.FALSE, eventClass, listener);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    private static boolean handleCancellableChatEvent(Object event) {
        return handleMessage(event);
    }

    private static void handleChatEvent(Object event) {
        if (handleMessage(event)) {
            cancel(event);
        }
    }

    private static boolean handleMessage(Object event) {
        String message = stringResult(event, "getMessage");
        return message != null && OmniCommand.handle(message);
    }

    private static void cancel(Object event) {
        if (invoke(event, "setCanceled", boolean.class, Boolean.TRUE)) {
            return;
        }
        invoke(event, "cancel");
    }

    private static String stringResult(Object target, String methodName) {
        try {
            Method method = findMethod(target.getClass(), methodName);
            if (method == null) {
                return null;
            }
            Object result = method.invoke(target);
            return result instanceof String ? (String) result : null;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
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

    private static boolean invoke(Object target, String methodName) {
        try {
            Method method = findMethod(target.getClass(), methodName);
            if (method == null) {
                return false;
            }
            method.invoke(target);
            return true;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameters) {
        Method method = findDeclaredMethod(owner, name, parameters);
        if (method != null) {
            return method;
        }

        for (Class<?> implemented : owner.getInterfaces()) {
            method = findMethod(implemented, name, parameters);
            if (method != null) {
                return method;
            }
        }

        Class<?> parent = owner.getSuperclass();
        return parent == null ? null : findMethod(parent, name, parameters);
    }

    private static Method findDeclaredMethod(Class<?> owner, String name, Class<?>... parameters) {
        for (Method method : owner.getDeclaredMethods()) {
            if (name.equals(method.getName()) && matches(method.getParameterTypes(), parameters)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static boolean matches(Class<?>[] actual, Class<?>[] expected) {
        if (actual.length != expected.length) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (!sameParameter(actual[i], expected[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameParameter(Class<?> actual, Class<?> expected) {
        if (actual.equals(expected)) {
            return true;
        }
        if (actual.isPrimitive()) {
            return primitiveWrapper(actual).equals(expected);
        }
        return false;
    }

    private static Class<?> primitiveWrapper(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }
}
