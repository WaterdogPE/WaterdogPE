/*
 * Copyright 2022 WaterdogTEAM
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

package dev.waterdog.waterdogpe.event;

import dev.waterdog.waterdogpe.utils.exceptions.EventException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.protocol.common.util.Preconditions;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Base Event Class
 * Events can be subscribed to in order to listen for activities on the proxy regarding players as well.
 * as other components
 */
public abstract class Event {

    private final List<CompletableFuture<Void>> completableFuture;
    private boolean cancelled = false;

    public Event() {
        this.completableFuture = this.isCompletable() ? Collections.synchronizedList(new ObjectArrayList<>()) : null;
    }

    public boolean isCancelled() {
        if (!(this instanceof CancellableEvent)) {
            throw new EventException("Event is not Cancellable");
        }
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        if (!(this instanceof CancellableEvent)) {
            throw new EventException("Event is not Cancellable");
        }
        this.cancelled = cancelled;
    }

    public void setCancelled() {
        if (!(this instanceof CancellableEvent)) {
            throw new EventException("Event is not Cancellable");
        }
        this.cancelled = true;
    }

    public void addCompletableFuture(CompletableFuture<Void> future) {
        Preconditions.checkArgument(this.isCompletable(), "Can not add complete future to event which is not @CompletableEvent or @AsyncEvent!");
        this.completableFuture.add(future);
    }

    protected List<CompletableFuture<Void>> getCompletableFutures() {
        Preconditions.checkArgument(this.isCompletable(), "Event is not @CompletableEvent or @AsyncEvent!");
        return this.completableFuture;
    }

    protected <T extends Event> void completeFuture(CompletableFuture<T> future) {
        Preconditions.checkArgument(this.isCompletable(), "Event is not @CompletableEvent or @AsyncEvent!");
        if (this.completableFuture.isEmpty()) {
            future.complete((T) this);
            return;
        }

        CompletableFuture.allOf(this.completableFuture.toArray(new CompletableFuture[0])).whenComplete((ignore, error) -> {
            if (error == null) {
                future.complete((T) this);
            } else {
                future.completeExceptionally(error);
            }
        });
    }

    public boolean isAsync() {
        return this.getClass().isAnnotationPresent(AsyncEvent.class);
    }

    public boolean isCompletable() {
        return this.getClass().isAnnotationPresent(CompletableEvent.class) || this.isAsync();
    }
}
