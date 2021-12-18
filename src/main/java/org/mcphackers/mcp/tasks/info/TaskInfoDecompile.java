package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskDecompile;

public class TaskInfoDecompile implements TaskInfo {

    public String title() {
        return "Decompiling";
    }

    public String successMsg() {
        return "DECOMPILE SUCCESSFUL!";
    }

    public String failMsg() {
        return "DECOMPILE FAILED!";
    }

    public Task newTask(int side) {
        return new TaskDecompile(side);
    }

    @Override
    public boolean isMultiThreaded() {
        return true;
    }

}
