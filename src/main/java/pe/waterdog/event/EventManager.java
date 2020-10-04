/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    public CompletableFuture<Event> callEvent(Event event) {
        ArrayList<Consumer<Event>> handlerList = this.handlerMap.get(event.getClass());
        if (event.getClass().isAnnotationPresent(AsyncEvent.class)) {
            return CompletableFuture.supplyAsync(() -> {
                if (handlerList != null){
                    for (Consumer<Event> eventHandler : handlerList) {
                        eventHandler.accept(event);
                    }
                }
                return event;
            });
        }

        if (handlerList != null) {
            for (Consumer<Event> eventHandler : handlerList) {
                eventHandler.accept(event);
            }
        }
        return null;
    }
}
