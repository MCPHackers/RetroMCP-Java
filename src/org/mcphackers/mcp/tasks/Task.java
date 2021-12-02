package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tools.ProgressInfo;

public abstract class Task {

	public Task(int side) {
	}
	
	public abstract void doTask() throws Exception;
	
	public abstract ProgressInfo getProgress();
}
