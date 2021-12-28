package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskCleanup;

public class TaskInfoCleanup extends TaskInfo {
    @Override
    public String title() {
        return "Cleaning up";
    }

    @Override
    public String successMsg() {
        return "CLEANUP SUCCESSFUL!";
    }

    @Override
    public String failMsg() {
        return "CLEANUP FAILED!";
    }

    @Override
    public Task newTask(int side) {
        return new TaskCleanup(this);
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }
}
