package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskUpdateMD5;

public class TaskInfoUpdateMD5 extends TaskInfo {
	@Override
	public String title() {
		return "Updating MD5";
	}

	@Override
	public String successMsg() {
		return "UPDATE MD5 SUCCESSFUL!";
	}

	@Override
	public String failMsg() {
		return "UPDATE MD5 FAILED!";
	}

	@Override
	public Task newTask(int side) {
		return new TaskUpdateMD5(side, this);
	}

	@Override
	public boolean isMultiThreaded() {
		return true;
	}
}
