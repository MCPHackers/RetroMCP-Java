package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskRecompile;

public class TaskInfoRecompile implements TaskInfo {
    @Override
    public String title() {
        return "Recompiling";
    }

    @Override
    public String successMsg() {
        return "RECOMPILE SUCCESSFUL!";
    }

    @Override
    public String failMsg() {
        return "RECOMPILE FAILED!";
    }

    @Override
    public Task newTask(int side) {
        return new TaskRecompile(side);
    }

    @Override
    public boolean isMultiThreaded() {
        return true;
    }
}
