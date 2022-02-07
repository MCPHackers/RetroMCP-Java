package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskCreatePatch;

public class TaskInfoCreatePatch extends TaskInfo {
    @Override
    public String title() {
        return "Creating patch";
    }

    @Override
    public String successMsg() {
        return "PATCH CREATION SUCCESSFUL";
    }

    @Override
    public String failMsg() {
        return "PATCH CREATION FAILED";
    }

    @Override
    public Task newTask(int side) {
        return new TaskCreatePatch(this);
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }
}
