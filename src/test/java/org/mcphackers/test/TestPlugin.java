package org.mcphackers.test;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.api.plugin.MCPPlugin;

public class TestPlugin extends MCPPlugin {
	@Override
	public void initializePlugin(MCP mcp) {
		System.out.println("Hello from test plugin!");
	}
}
