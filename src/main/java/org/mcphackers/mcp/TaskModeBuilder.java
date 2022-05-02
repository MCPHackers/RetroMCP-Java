package org.mcphackers.mcp;

import org.mcphackers.mcp.tasks.Task;

public class TaskModeBuilder {
	
	private String name;
	private String fullName;
	private String desc;
	public Class<? extends Task> taskClass;
	public TaskParameter[] params = new TaskParameter[] {};
	public Requirement requirement;

	public TaskModeBuilder setCmdName(String name) {
		this.name = name;
		return this;
	}

	public TaskModeBuilder setFullName(String name) {
		this.fullName = name;
		return this;
	}

	public TaskModeBuilder setDescription(String desc) {
		this.desc = desc;
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
		return new TaskMode(name, fullName, desc, taskClass, params, requirement);
	}
}
