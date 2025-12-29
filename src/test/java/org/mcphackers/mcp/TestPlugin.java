package org.mcphackers.mcp;

import java.util.logging.Logger;

import org.mcphackers.mcp.plugin.MCPPlugin;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskStaged;

public class TestPlugin implements MCPPlugin {
	private final Logger logger = Logger.getLogger("testPlugin");

    @Override
    public String pluginId() {
        return "test";
    }

    @Override
    public void init(MCP mcp) {
		logger.info("Test plugin has initialized!");
    }

    @Override
    public void onTaskEvent(TaskEvent event, Task task) {

    }

    @Override
    public void onMCPEvent(MCPEvent event, MCP mcp) {

    }

    @Override
    public void setTaskOverrides(TaskStaged task) {

    }
}
