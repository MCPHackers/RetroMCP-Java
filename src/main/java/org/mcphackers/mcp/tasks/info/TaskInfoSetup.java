package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskSetup;

public class TaskInfoSetup implements TaskInfo {
    @Override
    public String title() {
        return "Setup";
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
        return new TaskSetup();
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }
}
