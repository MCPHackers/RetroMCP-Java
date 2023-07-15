package org.mcphackers.mcp.api.task;

import org.mcphackers.mcp.MCP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TaskManager {
	private final List<Task> tasks = new ArrayList<>();

	public List<Task> getTasks() {
		return Collections.unmodifiableList(this.tasks);
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

	public Iterator<Task> iterator() {
		return this.tasks.iterator();
	}

	public void executeTask(MCP mcp, Task task, String[] args) {
		task.run(mcp, args);
	}
}
