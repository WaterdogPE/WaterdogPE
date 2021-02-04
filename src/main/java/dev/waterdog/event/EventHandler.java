/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.event;

import dev.waterdog.utils.exceptions.EventException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Internal EventHandler Class
 * Manages Event Calling, priorities, execution ordering.
 * Should not be modified if not necessary
 */
public class EventHandler {

    private final EventManager eventManager;
    private final Class<? extends Event> eventClass;

    private final EnumMap<EventPriority, ArrayList<Consumer<Event>>> priority2handlers = new EnumMap<>(EventPriority.class);

    public EventHandler(Class<? extends Event> eventClass, EventManager eventManager) {
        this.eventClass = eventClass;
        this.eventManager = eventManager;
    }

    public CompletableFuture<Event> handle(Event event) {
        if (!this.eventClass.isInstance(event)) {
            throw new EventException("Tried to handle invalid event type!");
        }

        if (event.getClass().isAnnotationPresent(AsyncEvent.class)) {
            return CompletableFuture.supplyAsync(() -> {
                for (EventPriority priority : EventPriority.values()) {
                    this.handlePriority(priority, event);
                }
                return event;
            }, this.eventManager.getThreadedExecutor());
        }

        for (EventPriority priority : EventPriority.values()) {
            this.handlePriority(priority, event);
        }
        return null;
    }

    private void handlePriority(EventPriority priority, Event event) {
        ArrayList<Consumer<Event>> handlerList = this.priority2handlers.get(priority);
        if (handlerList != null) {
            for (Consumer<Event> eventHandler : handlerList) {
                eventHandler.accept(event);
            }
        }
    }

    public void subscribe(Consumer<Event> handler, EventPriority priority) {
        List<Consumer<Event>> handlerList = this.priority2handlers.computeIfAbsent(priority, priority1 -> new ArrayList<>());

        //Check if event is already registered
        if (!handlerList.contains(handler)) {
            //Handler is not registered yet
            handlerList.add(handler);
        }
    }
}
