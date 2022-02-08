package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskBuild;

public class TaskInfoBuild extends TaskInfo {
	@Override
	public String title() {
		return "Building";
	}

	@Override
	public String successMsg() {
		return "BUILD SUCCESSFUL!";
	}

	@Override
	public String failMsg() {
		return "BUILD FAILED!";
	}

	@Override
	public Task newTask(int side) {
		return new TaskBuild(side, this);
	}

	@Override
	public boolean isMultiThreaded() {
		return true;
	}
}
