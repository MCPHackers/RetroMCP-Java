package org.mcphackers.mcp.tasks;

@FunctionalInterface
public interface TaskRunnable {

	void doTask() throws Exception;
}
