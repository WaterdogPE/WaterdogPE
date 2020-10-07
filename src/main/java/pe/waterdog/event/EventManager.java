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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EventManager {

    private final ExecutorService threadedExecutor;
    private final HashMap<Class<? extends Event>, EventHandler> handlerMap = new HashMap<>();

    public EventManager(){
        ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("WaterdogEvents Executor");
        this.threadedExecutor = Executors.newCachedThreadPool(builder.build());
    }

    public <T extends Event> void subscribe(Class<? extends Event> event, Consumer<T> handler) {
        this.subscribe(event, handler, EventPriority.NORMAL);
    }

    public <T extends Event> void subscribe(Class<? extends Event> event, Consumer<T> handler, EventPriority priority) {
        EventHandler eventHandler = this.handlerMap.computeIfAbsent(event, e -> new EventHandler(event, this));

        Consumer<Event> func = (Consumer<Event>) handler;
        eventHandler.subscribe(func, priority);
    }

    public CompletableFuture<Event> callEvent(Event event) {
        EventHandler eventHandler = this.handlerMap.computeIfAbsent(event.getClass(), e -> new EventHandler(event.getClass(), this));
        return eventHandler.handle(event);
    }

    public ExecutorService getThreadedExecutor() {
        return this.threadedExecutor;
    }
}
