package org.mcphackers.mcp.plugin;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tasks.Task;

public interface MCPPlugin {
	
	String pluginId();
	
	void init();
	
	void onTaskEvent(TaskEvent event, Task task);

	void onMCPEvent(MCPEvent event, MCP mcp);

	void setTaskOverrides(Task task);
	
	default TaskMode registerTask(String name, String fullName, Class<? extends Task> taskClass) {
		return new TaskMode(name, fullName, taskClass);
	}
	
	default TaskMode registerTask(String name, String fullName, Class<? extends Task> taskClass, TaskParameter[] params) {
		return new TaskMode(name, fullName, taskClass, params);
	}
	
	default TaskMode registerTask(String name, String fullName, String desc, Class<? extends Task> taskClass) {
		return new TaskMode(name, fullName, desc, taskClass);
	}
	
	default TaskMode registerTask(String name, String fullName, String desc, Class<? extends Task> taskClass, TaskParameter[] params) {
		return new TaskMode(name, fullName, desc, taskClass, params);
	}
	
	public enum TaskEvent {
		//TODO Custom events
		PRE_TASK,
		POST_TASK,
		TASK_STEP;
	}
	
	public enum MCPEvent {
		//TODO Custom events
		STARTED_TASKS,
		FINISHED_TASKS,
		ENV_STARTUP;
	}
	
}
