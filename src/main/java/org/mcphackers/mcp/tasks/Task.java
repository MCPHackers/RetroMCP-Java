package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.SideThread;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.ProgressInfo;

public abstract class Task {
	
	protected int step = 0;
	protected final int side;
	protected final TaskInfo info;
	protected SideThread ownerThread;
	
	protected Task(int side, TaskInfo info) {
		this.side = side;
		this.info = info;
	}

	public abstract void doTask() throws Exception;

	public void doTask(SideThread thread) throws Exception {
		ownerThread = thread;
		doTask();
	}

    public ProgressInfo getProgress() {
    	return new ProgressInfo("Idle", (step > 0 ? 1 : 0), 1);
    }
    
    protected boolean step() throws Exception {
    	if(ownerThread != null && ownerThread.stopped()) {
    		return false;
    	}
    	step++;
    	return true;
    }
}
