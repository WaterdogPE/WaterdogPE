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

package dev.waterdog.waterdogpe.scheduler;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Simple implementation of task which allows calling own callbacks with provided result after completition.
 * This task is especially handy when it comes to delayed tasks because
 * CompletableFuture is not providing us this kind of API at the moment.
 * @param <T> expected result
 */
public class CallbackTask<T> extends Task {

    private final Supplier<T> task;
    private List<Consumer<T>> callbacks;

    public CallbackTask(Supplier<T> task) {
        this.task = task;
    }

    public CallbackTask(Supplier<T> task, Consumer<T> callback) {
        this.task = task;
        this.addCallback(callback);
    }

    @Override
    public void onRun(int currentTick) {
        this.complete(this.task.get());
    }

    @Override
    public void onCancel() {
        this.complete(null);
    }

    private void complete(T result) {
        if (this.callbacks != null && !this.callbacks.isEmpty()) {
            for (Consumer<T> callback : this.callbacks) {
                callback.accept(result);
            }
        }
    }

    public void addCallback(Consumer<T> callback) {
        if (this.callbacks == null) {
            this.callbacks = Collections.synchronizedList(new ObjectArrayList<>());
        }
        this.callbacks.add(callback);
    }
}
