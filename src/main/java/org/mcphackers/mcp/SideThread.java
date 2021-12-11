package org.mcphackers.mcp;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tools.ProgressInfo;

public class SideThread extends Thread {

    private final int side;
    private final Task task;
    public Exception exception;

    public SideThread(int i, Task t) {
        side = i;
        exception = null;
        task = t;
    }

    public void run() {
        try {
            task.doTask();
        } catch (Exception e) {
            exception = e;
            Thread.currentThread().interrupt();
        }
    }

    public String getSideName() {
        if (side == 0)
            return "Client";
        if (side == 1)
            return "Server";
        return "Unknown";
    }

    public ProgressInfo getInfo() {
        if (!isAlive() && exception == null)
            return new ProgressInfo("Done!", 1, 1);
        else
            return task.getProgress();
    }
}
