package org.mcphackers.mcp;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tools.ProgressInfo;

public class SideThread extends Thread {

    private boolean stopThread;
	private final int side;
    private final Task task;
    public volatile Exception exception;

    public SideThread(int i, Task t) {
    	super(getSideName(i) + " thread");
        side = i;
        exception = null;
        task = t;
    }

    public void run() {
        try {
            task.doTask(this);
        } catch (Exception e) {
            exception = e;
        }
    }
    
    public void stopThread() {
    	stopThread = true;
    }
    
    public boolean stopped() {
    	return stopThread;
    }

    public String getSideName() {
    	return getSideName(side);
    }

    public static String getSideName(int side) {
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
