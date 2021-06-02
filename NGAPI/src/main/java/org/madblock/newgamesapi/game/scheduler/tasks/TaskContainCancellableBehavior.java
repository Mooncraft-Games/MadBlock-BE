package org.madblock.newgamesapi.game.scheduler.tasks;

import cn.nukkit.scheduler.Task;
import org.madblock.newgamesapi.game.GameHandler;

import java.util.function.Consumer;

public class TaskContainCancellableBehavior extends Task {

    public GameHandler handler;
    public Consumer<Task> behavior;

    private String token;

    public TaskContainCancellableBehavior(GameHandler handler, Consumer<Task> behavior, String token) {
        this.handler = handler;
        this.behavior = behavior;
        this.token = token;
    }

    @Override
    public void onRun(int currentTick) {
        if(handler.getGameState() == GameHandler.GameState.MAIN_LOOP) {
            behavior.accept(this);
        } else {
            this.cancel();
        }
    }

    @Override
    public void onCancel() {
        handler.getGameScheduler().removeTaskHandler(this.getHandler(), token);
    }
}
