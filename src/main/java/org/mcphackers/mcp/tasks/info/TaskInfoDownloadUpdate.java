package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskDownloadUpdate;

public class TaskInfoDownloadUpdate extends TaskInfo {
    @Override
    public String title() {
        return "Setting up";
    }

    @Override
    public String successMsg() {
        return "UP TO DATE!";
    }

    @Override
    public String failMsg() {
        return "COULD NOT FETCH LATEST RELEASE!";
    }

    @Override
    public Task newTask(int side) {
        return new TaskDownloadUpdate(this);
    }

    @Override
    public boolean isMultiThreaded() {
        return false;
    }
}
