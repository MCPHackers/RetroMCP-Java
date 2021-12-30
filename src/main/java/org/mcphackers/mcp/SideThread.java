package org.mcphackers.mcp;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tools.ProgressInfo;

public class SideThread extends Thread {

	private final int side;
    private final Task task;
    public volatile Exception exception;
    
    public static final int CLIENT = 0;
    public static final int SERVER = 1;

    public SideThread(int i, Task t) {
    	super(getSideName(i) + " thread");
        side = i;
        task = t;
        exception = null;
    }

    public void run() {
        try {
            task.doTask();
        } catch (Exception e) {
            exception = e;
        }
    }

    public String getSideName() {
    	return getSideName(side);
    }

    public static String getSideName(int side) {
        if (side == CLIENT)
            return "Client";
        if (side == SERVER)
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
