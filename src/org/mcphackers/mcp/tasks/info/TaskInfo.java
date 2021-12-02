package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;

public interface TaskInfo {

	public abstract String title();
	
	public abstract String successMsg();
	
	public abstract String failMsg();
	
	public abstract Task newTask(int side);
}
