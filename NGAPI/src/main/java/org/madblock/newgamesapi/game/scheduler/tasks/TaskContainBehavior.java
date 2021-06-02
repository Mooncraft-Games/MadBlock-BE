package org.madblock.newgamesapi.game.scheduler.tasks;

import cn.nukkit.scheduler.Task;
import org.madblock.newgamesapi.game.GameHandler;

public class TaskContainBehavior extends Task {

    public GameHandler handler;
    public Runnable behavior;

    private String token;

    public TaskContainBehavior(GameHandler handler, Runnable behavior, String token) {
        this.handler = handler;
        this.behavior = behavior;
        this.token = token;
    }

    @Override
    public void onRun(int currentTick) {
        if(handler.getGameState() != GameHandler.GameState.END) {
            behavior.run();
        } else {
            this.cancel();
        }
    }

    @Override
    public void onCancel() {
        handler.getGameScheduler().removeTaskHandler(this.getHandler(), token);
    }
}
