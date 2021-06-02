package org.madblock.newgamesapi.game.scheduler;

import cn.nukkit.scheduler.Task;
import cn.nukkit.scheduler.TaskHandler;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.scheduler.tasks.TaskContainBehavior;
import org.madblock.newgamesapi.game.scheduler.tasks.TaskContainCancellableBehavior;

import java.util.ArrayList;
import java.util.function.Consumer;

public class GameScheduler {

    private GameHandler handler;
    private ArrayList<TaskHandler> taskHandlers;

    private String token;

    public GameScheduler(GameHandler handler, String token) {
        this.handler = handler;
        this.taskHandlers = new ArrayList<>();
        this.token = token;
    }

    public TaskHandler registerGameTask(Runnable method){ return registerGameTask(method, 0, 0); }
    public TaskHandler registerGameTask(Runnable method, int delay){ return registerGameTask(method, delay, 0); }
    public TaskHandler registerGameTask(Runnable method, int delay, int repeatInterval){
        TaskHandler taskHandler = NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedRepeatingTask(new TaskContainBehavior(handler, method, token), delay, repeatInterval, false);
        taskHandlers.add(taskHandler);
        return taskHandler;
    }

    public TaskHandler registerSelfCancellableGameTask(Consumer<Task> method){ return registerSelfCancellableGameTask(method, 0, 0); }
    public TaskHandler registerSelfCancellableGameTask(Consumer<Task> method, int delay){ return registerSelfCancellableGameTask(method, delay, 0); }
    public TaskHandler registerSelfCancellableGameTask(Consumer<Task> method, int delay, int repeatInterval){
        TaskHandler taskHandler = NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedRepeatingTask(new TaskContainCancellableBehavior(handler, method, token), delay, repeatInterval, false);
        taskHandlers.add(taskHandler);
        return taskHandler;
    }

    public void removeTaskHandler(TaskHandler taskHandler, String token){
        if(this.token.equals(token)){
            taskHandlers.remove(taskHandler);
        }
    }

    public ArrayList<TaskHandler> getTaskHandlers() {
        return taskHandlers;
    }
}
