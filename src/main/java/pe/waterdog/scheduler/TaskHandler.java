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

package pe.waterdog.scheduler;

import pe.waterdog.logger.MainLogger;

public class TaskHandler {

    private final int taskId;
    private final boolean async;

    private final Runnable task;

    private int delay;
    private int period;

    private int lastRunTick;
    private int nextRunTick;

    private boolean cancelled;

    public TaskHandler(Runnable task, int taskId, boolean async){
        this.task = task;
        this.taskId = taskId;
        this.async = async;
    }

    public void onRun(int currentTick){
        this.lastRunTick = currentTick;
        try {
            this.task.run();
        }catch (Exception e){
            MainLogger.getLogger().error("Exception while running task!", e);
        }
    }

    public void cancel(){
        if (this.cancelled){
            return;
        }

        if (this.task instanceof Task){
            ((Task) task).onCancel();
        }
        this.cancelled = true;
    }

    public boolean calculateNextTick(int currentTick){
        if (this.isCancelled() || !this.isRepeating()){
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

    public Runnable getTask() {
        return this.task;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return this.delay;
    }

    public boolean isDelayed() {
        return this.delay > 0;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getPeriod() {
        return this.period;
    }

    public boolean isRepeating() {
        return this.period > 0;
    }

    public int getLastRunTick() {
        return this.lastRunTick;
    }

    public void setNextRunTick(int nextRunTick) {
        this.nextRunTick = nextRunTick;
    }

    public int getNextRunTick() {
        return this.nextRunTick;
    }
}
