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

import dev.waterdog.waterdogpe.logger.MainLogger;

public class TaskHandler<T extends Runnable> {

    private final int taskId;
    private final boolean async;

    private final T task;

    private int delay;
    private int period;

    private int lastRunTick;
    private int nextRunTick;

    private boolean cancelled;

    public TaskHandler(T task, int taskId, boolean async) {
        this.task = task;
        if (task instanceof Task) {
            ((Task) task).setHandler((TaskHandler<Task>) this);
        }
        this.taskId = taskId;
        this.async = async;
    }

    public void onRun(int currentTick) {
        this.lastRunTick = currentTick;
        try {
            this.task.run();
        } catch (Throwable t) {
            if (this.task instanceof Task) {
                ((Task) this.task).onError(t);
            } else {
                MainLogger.getLogger().error("Exception while running task!", t);
            }
        }
    }

    public void cancel() {
        if (this.cancelled) {
            return;
        }

        if (this.task instanceof Task) {
            ((Task) this.task).onCancel();
        }
        this.cancelled = true;
    }

    public boolean calculateNextTick(int currentTick) {
        if (this.isCancelled() || !this.isRepeating()) {
            return false;
        }
        this.nextRunTick = currentTick + this.period;
        return true;
    }

    public int getTaskId() {
        return this.taskId;
    }

    public boolean isAsync() {
        return this.async;
    }

    public T getTask() {
        return this.task;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public int getDelay() {
        return this.delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean isDelayed() {
        return this.delay > 0;
    }

    public int getPeriod() {
        return this.period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public boolean isRepeating() {
        return this.period > 0;
    }

    public int getLastRunTick() {
        return this.lastRunTick;
    }

    public int getNextRunTick() {
        return this.nextRunTick;
    }

    public void setNextRunTick(int nextRunTick) {
        this.nextRunTick = nextRunTick;
    }
}
