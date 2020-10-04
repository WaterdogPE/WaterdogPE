package pe.waterdog.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EventManager {

    private final HashMap<Class<? extends Event>, ArrayList<Consumer<Event>>> handlerMap = new HashMap<>();

    public <T extends Event> void subscribe(Class<? extends Event> event, Consumer<T> handler) {
        ArrayList<Consumer<Event>> handlerList = this.handlerMap.get(event);
        Consumer<Event> func = (Consumer<Event>) handler;
        if (handlerList != null) {
            // Event is registered already
            if (!handlerList.contains(handler)) {
                // Handler is not registered yet
                handlerList.add(func);
            }
        } else {
            handlerList = new ArrayList<>();
            handlerList.add(func);
            this.handlerMap.put(event, handlerList);
        }
    }

    public CompletableFuture<Void> callEvent(Event event) {
        ArrayList<Consumer<Event>> handlerList = this.handlerMap.get(event.getClass());
        if (handlerList != null) {
            if (event.getClass().isAnnotationPresent(AsyncEvent.class)) {
                return CompletableFuture.runAsync(() -> {
                    for (Consumer<Event> eventHandler : handlerList) {
                        eventHandler.accept(event);
                    }
                });
            } else {
                for (Consumer<Event> eventHandler : handlerList) {
                    eventHandler.accept(event);
                }
            }
        }
        return null;
    }
}
