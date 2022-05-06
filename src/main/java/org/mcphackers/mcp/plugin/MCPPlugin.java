package org.mcphackers.mcp.plugin;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.Task;

public interface MCPPlugin {
	
	String pluginId();
	
	void init();
	
	void onTaskEvent(TaskEvent event, Task task);

	void onMCPEvent(MCPEvent event, MCP mcp);

	void setTaskOverrides(Task task);
	
	public enum TaskEvent {
		PRE_TASK,
		POST_TASK,
		TASK_STEP;
	}
	
	public enum MCPEvent {
		STARTED_TASKS,
		FINISHED_TASKS,
		ENV_STARTUP;
	}
	
}
