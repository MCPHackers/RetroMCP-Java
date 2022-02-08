package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskDownloadUpdate;

public class TaskInfoDownloadUpdate extends TaskInfo {
	@Override
	public String title() {
		return "Updating";
	}

	@Override
	public String successMsg() {
		return "UP TO DATE!";
	}

	@Override
	public String failMsg() {
		return "COULD NOT UPDATE!";
	}

	@Override
	public Task newTask(int side) {
		return new TaskDownloadUpdate(this);
	}

	@Override
	public boolean isMultiThreaded() {
		return false;
	}
}
