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

import pe.waterdog.utils.exceptions.EventException;

/**
 * Base Event Class
 * Events can be subscribed to in order to listen for activities on the proxy regarding players as well.
 * as other components
 */
public abstract class Event {

    private boolean cancelled = false;

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
}
