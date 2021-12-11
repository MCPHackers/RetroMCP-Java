package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tools.ProgressInfo;

public abstract class Task {

	protected int side;

	public Task(int side) {
		this.side = side;
	}
	
	public abstract void doTask() throws Exception;
	
	public abstract ProgressInfo getProgress();
}
