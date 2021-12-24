package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskDecompile;

public class TaskInfoDecompile extends TaskInfo {
    @Override
    public String title() {
        return "Decompiling";
    }

    @Override
    public String successMsg() {
        return "DECOMPILE SUCCESSFUL!";
    }

    @Override
    public String failMsg() {
        return "DECOMPILE FAILED!";
    }

    @Override
    public Task newTask(int side) {
        return new TaskDecompile(side, this);
    }

    @Override
    public boolean isMultiThreaded() {
        return true;
    }
}
