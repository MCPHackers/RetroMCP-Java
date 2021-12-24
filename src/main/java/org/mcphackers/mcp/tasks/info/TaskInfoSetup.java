package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskSetup;

public class TaskInfoSetup extends TaskInfo {
    @Override
    public String title() {
        return "Setting up";
    }

    @Override
    public String successMsg() {
        return "SETUP SUCCESSFUL!";
    }

    @Override
    public String failMsg() {
        return "SETUP FAILED!";
    }

    @Override
    public Task newTask(int side) {
        return new TaskSetup(this);
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }
}
