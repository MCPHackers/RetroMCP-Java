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
	void init(MCP mcp);

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
	 *
	 * @param task the task with stages to override
	 */
	void setTaskOverrides(TaskStaged task);

	enum TaskEvent {
		/**
		 * Called before a task is executed
		 */
		PRE_TASK,
		/**
		 * Called after the task has executed
		 */
		POST_TASK,
		/**
		 * Called each time a task moves to the next execution stage
		 */
		TASK_STEP
	}

	enum MCPEvent {
		/**
		 * Called when a task begins execution
		 */
		STARTED_TASKS,
		/**
		 * Called when all tasks have finished execution
		 */
		FINISHED_TASKS,
		/**
		 * Called when RMCP starts up
		 */
		ENV_STARTUP
	}

}
