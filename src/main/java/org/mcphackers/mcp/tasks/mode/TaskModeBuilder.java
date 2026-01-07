package org.mcphackers.mcp.tasks.mode;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.mode.TaskMode.Requirement;

public class TaskModeBuilder {

	private String name;
	private boolean usesProgressBars = true;
	private Class<? extends Task> taskClass;
	private TaskParameter[] params = new TaskParameter[]{};
	private Requirement requirement;

	/**
	 * @param name Task name to be set.
	 * @return Current {@link TaskModeBuilder} instance
	 */
	public TaskModeBuilder setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @param enabled Determines if progress bars are enabled or disabled.
	 * @return Current {@link TaskModeBuilder} instance
	 */
	public TaskModeBuilder setProgressBars(boolean enabled) {
		this.usesProgressBars = enabled;
		return this;
	}

	/**
	 * @param taskClass Task class to be ran
	 * @return Current {@link TaskModeBuilder} instance
	 */
	public TaskModeBuilder setTaskClass(Class<? extends Task> taskClass) {
		this.taskClass = taskClass;
		return this;
	}

	/**
	 * @param params Task parameters to be used
	 * @return Current {@link TaskModeBuilder} instance
	 */
	public TaskModeBuilder setParameters(TaskParameter[] params) {
		this.params = params;
		return this;
	}

	/**
	 * @param condition Task requirement for task to become executable
	 * @return Current {@link TaskModeBuilder} instance
	 */
	public TaskModeBuilder addRequirement(Requirement condition) {
		this.requirement = condition;
		return this;
	}

	/**
	 * @return Creates a {@link TaskMode} instance from {@link TaskModeBuilder}
	 */
	public TaskMode build() {
		return new TaskMode(this.name, this.taskClass, this.params, this.usesProgressBars, this.requirement);
	}
}
