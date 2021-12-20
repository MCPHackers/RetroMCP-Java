package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.SideThread;
import org.mcphackers.mcp.tools.ProgressInfo;

public abstract class Task {
	
	protected int step = 0;
	protected SideThread ownerThread;

	public abstract void doTask() throws Exception;

	public void doTask(SideThread thread) throws Exception {
		ownerThread = thread;
		doTask();
	}

    public ProgressInfo getProgress() {
    	return new ProgressInfo("Idle", (step > 0 ? 1 : 0), 1);
    }
    
    protected void step() throws Exception {
    	step++;
    	if(ownerThread != null && ownerThread.stopped()) {
    		throw new Exception("Stopping execution");
    	}
    }
}
