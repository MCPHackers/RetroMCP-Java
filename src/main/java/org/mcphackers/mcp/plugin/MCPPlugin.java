package org.mcphackers.mcp.plugin;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskStaged;

public interface MCPPlugin {
	
	/**
	 * @return A unique string id
	 */
	String pluginId();

	/**
	 * Called when the plugin is loaded
	 */
	void init();

	/**
	 * Called whenever a certain task event happens inside of task instance
	 */
	void onTaskEvent(TaskEvent event, Task task);

	/**
	 * Called whenever a certain task event happens inside of task instance
	 */
	void onMCPEvent(MCPEvent event, MCP mcp);

	/**
	 * Called whenever an instance of TaskStaged starts execution.
	 * Use {@link TaskStaged#overrideStage(int, org.mcphackers.mcp.tasks.TaskRunnable)} 
	 * to replace one of the stages.
	 * @param task the task with stages to override
	 */
	void setTaskOverrides(TaskStaged task);
	
	public enum TaskEvent {
		PRE_TASK, /** Called before the task is executed */
		POST_TASK, /** Called after the task was completed */
		TASK_STEP; /** Called each time TaskStaged moves to another stage */
	}
	
	public enum MCPEvent {
		STARTED_TASKS, /** Called when one or multiple tasks began execution */
		FINISHED_TASKS, /** Called when all tasks finished execution */
		ENV_STARTUP; /** Called when MCP starts up */
	}
	
}
