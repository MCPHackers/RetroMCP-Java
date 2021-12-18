package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskDecompile;

public class TaskInfoDecompile implements TaskInfo {
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
        return new TaskDecompile(side);
    }

    @Override
    public boolean isMultiThreaded() {
        return true;
    }

}
