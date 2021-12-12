package org.mcphackers.mcp.tasks.info;

import org.mcphackers.mcp.tasks.Task;

public interface TaskInfo {

    String title();

    String successMsg();

    String failMsg();

    Task newTask(int side);

    boolean hasServerThread();
}
