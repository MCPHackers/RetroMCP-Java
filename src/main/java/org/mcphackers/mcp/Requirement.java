package org.mcphackers.mcp;

import org.mcphackers.mcp.tasks.Task.Side;

@FunctionalInterface
public interface Requirement {
	boolean get(MCP mcp, Side side);
}
