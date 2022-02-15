package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskTest;

public class TaskInfoTest extends TaskInfo {
	@Override
	public String title() {
		return "Testing";
	}

	@Override
	public String successMsg() {
		return "ALL TESTS HAVE PASSED!";
	}

	@Override
	public String failMsg() {
		return "TESTS FAILED!";
	}

	@Override
	public Task newTask(int side) {
		return new TaskTest(this);
	}

	@Override
	public boolean isMultiThreaded() {
		return false;
	}
}
