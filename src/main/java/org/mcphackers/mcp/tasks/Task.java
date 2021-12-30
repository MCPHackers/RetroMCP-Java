package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.ProgressInfo;

public abstract class Task {
	
	protected int step = 0;
	protected final int side;
	protected final TaskInfo info;
	
	protected Task(int side, TaskInfo info) {
		this.side = side;
		this.info = info;
	}

	public abstract void doTask() throws Exception;

    public ProgressInfo getProgress() {
    	return new ProgressInfo("Idle", (step > 0 ? 1 : 0), 1);
    }
    
    protected void step() {
    	step++;
    }
}
