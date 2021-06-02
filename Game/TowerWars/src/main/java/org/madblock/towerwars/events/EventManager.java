package org.madblock.towerwars.events;

import org.madblock.towerwars.TowerWarsPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles all GameEvents for a GameBehavior
 */
public class EventManager {

    private final Set<GameListener> listeners = new HashSet<>();

    public void register(GameListener listener) {
        this.listeners.add(listener);
    }

    public void unregister(GameListener listener) {
        this.listeners.remove(listener);
    }

    public void callEvent(GameEvent event) {
        for (GameListener listener : this.listeners) {
            for (Method method : getValidEventMethods(listener.getClass(), event)) {
                try {
                    method.invoke(this, event);
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    TowerWarsPlugin.get().getLogger().error("An error occurred while triggering a GameEvent: " + event.getClass().getName(), exception);
                }

                if (event.isCancelled()) {
                    break;
                }
            }
        }
    }

    private static List<Method> getValidEventMethods(Class<?> clazz, GameEvent event) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> method.isAnnotationPresent(TowerWarsEventHandler.class))
                .filter(method -> method.getParameterTypes().length == 1 && method.getParameterTypes()[0].getName().equals(event.getClass().getName()))
                .sorted((methodA, methodB) -> {
                    TowerWarsEventHandler.Priority priorityA = methodA.getAnnotation(TowerWarsEventHandler.class).callPriority();
                    TowerWarsEventHandler.Priority priorityB = methodB.getAnnotation(TowerWarsEventHandler.class).callPriority();
                    return priorityB.compareTo(priorityA);
                })
                .collect(Collectors.toList());
    }

}
