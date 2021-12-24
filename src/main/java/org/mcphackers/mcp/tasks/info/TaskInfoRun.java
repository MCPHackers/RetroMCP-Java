package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskRun;

public class TaskInfoRun extends TaskInfo {
	private final int side;
	
	public TaskInfoRun(int side) {
		this.side = side;
	}
	
    @Override
    public String title() {
        return "Running " + (side == 1 ? "Server" : "Client");
    }

    @Override
    public String successMsg() {
        return null;
    }

    @Override
    public String failMsg() {
        return "CRASH DETECTED!";
    }

    @Override
    public Task newTask(int side) {
        return new TaskRun(this.side, this);
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }
}
