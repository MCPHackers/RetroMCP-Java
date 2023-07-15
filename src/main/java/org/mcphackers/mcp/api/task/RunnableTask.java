package org.mcphackers.mcp.api.task;

import org.mcphackers.mcp.MCP;

@FunctionalInterface
public interface RunnableTask {
	void run(MCP mcp, String[] args);
}
