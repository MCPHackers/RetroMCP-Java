package org.mcphackers.mcp.api.command.task;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {
	private final List<Task> tasks = new ArrayList<>();

	public List<Task> getTasks() {
		return this.tasks;
	}

	public void clearTasks() {
		this.tasks.clear();
	}

	public boolean addTask(Task task) {
		return this.tasks.add(task);
	}

	public boolean removeTask(Task task) {
		return this.tasks.remove(task);
	}
}
