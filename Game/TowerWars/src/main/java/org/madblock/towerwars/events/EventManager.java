package org.madblock.towerwars.events;

import org.madblock.towerwars.TowerWarsPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
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
        this.callGameListeners(this.listeners, event);
    }

    public void callEvent(Collection<GameListener> objects, GameEvent event) {
        this.callGameListeners(objects, event);
    }

    private void callGameListeners(Collection<GameListener> listeners, GameEvent event) {
        for (GameListener listener : listeners) {
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
