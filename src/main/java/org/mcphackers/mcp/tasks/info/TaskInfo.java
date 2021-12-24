package org.mcphackers.mcp.tasks.info;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.tasks.Task;

public abstract class TaskInfo {
	private List<String> errors = new ArrayList<String>();

	public abstract String title();

	public abstract String successMsg();

	public abstract String failMsg();

	public abstract Task newTask(int side);

	public abstract boolean isMultiThreaded();

	public List<String> getErrorList() {
		return this.errors;
	}

	public void addError(String err) {
		this.errors.add(err);
	}
}
