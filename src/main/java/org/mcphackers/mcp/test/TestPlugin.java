package org.mcphackers.mcp.test;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.api.command.Command;
import org.mcphackers.mcp.api.command.CommandManager;
import org.mcphackers.mcp.api.logging.LoggingManager;
import org.mcphackers.mcp.api.plugin.MCPPlugin;

public class TestPlugin extends MCPPlugin {
	@Override
	public void initializePlugin(MCP mcp) {
		LoggingManager loggingManager = mcp.getLoggingManager();
		loggingManager.info("Hello from test plugin!");
		CommandManager commandManager = mcp.getCommandManager();
		Command helpCommand = new Command("help", "command.help_long_desc", "command.help_desc", () -> {
			System.out.println("HELP!");
		});
		commandManager.addCommand(helpCommand);
	}
}
