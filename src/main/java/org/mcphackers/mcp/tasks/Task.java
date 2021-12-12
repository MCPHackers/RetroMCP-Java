package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tools.ProgressInfo;

public interface Task {

    void doTask() throws Exception;

    ProgressInfo getProgress();
}
