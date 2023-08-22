package org.mcphackers.mcp.tasks.mode;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.mode.TaskMode.Requirement;

public class TaskModeBuilder {

	private String name;
	private boolean usesProgressBars = true;
	private Class<? extends Task> taskClass;
	private TaskParameter[] params = new TaskParameter[]{};
	private Requirement requirement;

	public TaskModeBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public TaskModeBuilder setProgressBars(boolean enabled) {
		this.usesProgressBars = enabled;
		return this;
	}

	public TaskModeBuilder setTaskClass(Class<? extends Task> taskClass) {
		this.taskClass = taskClass;
		return this;
	}

	public TaskModeBuilder setParameters(TaskParameter[] params) {
		this.params = params;
		return this;
	}

	public TaskModeBuilder addRequirement(Requirement condition) {
		this.requirement = condition;
		return this;
	}

	public TaskMode build() {
		return new TaskMode(this.name, this.taskClass, this.params, this.usesProgressBars, this.requirement);
	}
}
