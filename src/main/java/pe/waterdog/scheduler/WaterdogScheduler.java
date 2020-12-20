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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pe.waterdog.ProxyServer;
import pe.waterdog.utils.exceptions.SchedulerException;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WaterdogScheduler {

    private static WaterdogScheduler instance;
    private final ProxyServer proxy;

    private final ExecutorService threadedExecutor;

    private final Map<Integer, TaskHandler> taskHandlerMap = new ConcurrentHashMap<>();
    private final Map<Integer, LinkedList<TaskHandler>> assignedTasks = new ConcurrentHashMap<>();
    private final LinkedList<TaskHandler> pendingTasks = new LinkedList<>();

    private final AtomicInteger currentId = new AtomicInteger();

    public WaterdogScheduler(ProxyServer proxy) {
        if (instance != null) {
            throw new RuntimeException("Scheduler was already initialized!");
        }
        instance = this;
        this.proxy = proxy;

        ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("WaterdogScheduler Executor");
        int idleThreads = this.proxy.getConfiguration().getIdleThreads();
        this.threadedExecutor = new ThreadPoolExecutor(idleThreads, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), builder.build());
    }

    public static WaterdogScheduler getInstance() {
        return instance;
    }

    public TaskHandler scheduleAsync(Runnable task) {
        return this.scheduleTask(task, true);
    }

    public TaskHandler scheduleTask(Runnable task, boolean async) {
        return this.addTask(task, 0, 0, async);
    }

    public TaskHandler scheduleDelayed(Runnable task, int delay) {
        return this.scheduleDelayed(task, delay, false);
    }

    public TaskHandler scheduleDelayed(Runnable task, int delay, boolean async) {
        return this.addTask(task, delay, 0, async);
    }

    public TaskHandler scheduleRepeating(Runnable task, int period) {
        return this.scheduleRepeating(task, period, false);
    }

    public TaskHandler scheduleRepeating(Runnable task, int period, boolean async) {
        return this.addTask(task, 0, period, async);
    }

    public TaskHandler scheduleDelayedRepeating(Runnable task, int delay, int period) {
        return this.scheduleDelayedRepeating(task, delay, period, false);
    }

    public TaskHandler scheduleDelayedRepeating(Runnable task, int delay, int period, boolean async) {
        return this.addTask(task, delay, period, async);
    }

    public TaskHandler addTask(Runnable task, int delay, int period, boolean async) {
        if (delay < 0 || period < 0) {
            throw new SchedulerException("Attempted to register a task with negative delay or period!");
        }

        int currentTick = this.getCurrentTick();
        int taskId = this.currentId.getAndIncrement();

        TaskHandler handler = new TaskHandler(task, taskId, async);
        handler.setDelay(delay);
        handler.setPeriod(period);
        handler.setNextRunTick(handler.isDelayed() ? currentTick + delay : currentTick);

        if (task instanceof Task) {
            ((Task) task).setHandler(handler);
        }

        this.pendingTasks.add(handler);
        this.taskHandlerMap.put(taskId, handler);
        return handler;
    }

    public void onTick(int currentTick) {
        // 1. Assign all tasks to queue by nextRunTick
        TaskHandler task;
        while ((task = this.pendingTasks.poll()) != null) {
            int tick = Math.max(currentTick, task.getNextRunTick());
            this.assignedTasks.computeIfAbsent(tick, integer -> new LinkedList<>()).add(task);
        }

        // 2. Run all tasks assigned to current tick
        LinkedList<TaskHandler> queued = this.assignedTasks.remove(currentTick);
        if (queued == null) return;

        for (TaskHandler taskHandler : queued) {
            this.runTask(taskHandler, currentTick);
        }
    }

    private void runTask(TaskHandler taskHandler, int currentTick) {
        if (taskHandler.isCancelled()) {
            this.taskHandlerMap.remove(taskHandler.getTaskId());
            return;
        }

        if (taskHandler.isAsync()) {
            this.threadedExecutor.execute(() -> taskHandler.onRun(currentTick));
        } else {
            taskHandler.onRun(currentTick);
        }

        if (taskHandler.calculateNextTick(currentTick)) {
            this.pendingTasks.add(taskHandler);
            return;
        }

        TaskHandler removed = this.taskHandlerMap.remove(taskHandler.getTaskId());
        removed.cancel();
    }

    public void shutdown() {
        this.proxy.getLogger().debug("Scheduler shutdown initialized!");
        this.threadedExecutor.shutdown();

        int count = 25;
        while (!this.threadedExecutor.isTerminated() && count-- > 0) {
            try {
                this.threadedExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }


    public int getCurrentTick() {
        return this.proxy.getCurrentTick();
    }
}
