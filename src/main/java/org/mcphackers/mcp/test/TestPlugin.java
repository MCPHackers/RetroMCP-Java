package org.mcphackers.mcp.test;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.api.logging.LoggingManager;
import org.mcphackers.mcp.api.plugin.MCPPlugin;
import org.mcphackers.mcp.api.task.Task;
import org.mcphackers.mcp.api.task.TaskManager;

import java.util.Arrays;

public class TestPlugin extends MCPPlugin {
	@Override
	public void initializePlugin(MCP mcp) {
		LoggingManager loggingManager = mcp.getLoggingManager();
		loggingManager.info("Hello from test plugin!");
		TaskManager taskManager = mcp.getTaskManager();
		Task task = new Task("setup", "task.setup_long_desc", "task.setup_short_desc") {
			@Override
			public void run(MCP mcp, String[] args) {
				System.out.println("Hello!");
				System.out.println(Arrays.toString(args));
			}
		};
		taskManager.addTask(task);
	}
}
