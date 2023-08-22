package org.mcphackers.mcp;

import org.mcphackers.mcp.plugin.MCPPlugin;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskMergeMappings;
import org.mcphackers.mcp.tasks.TaskStaged;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskModeBuilder;
import org.mcphackers.mcp.tasks.mode.TaskParameter;

import java.util.List;

public class TestPlugin implements MCPPlugin {
    @Override
    public String pluginId() {
        return "test";
    }

    @Override
    public void init() {
		System.out.println("Test plugin has initialized!");
		TaskMode mergeMappingsTask = new TaskModeBuilder()
				.setName("mergemappings")
				.setTaskClass(TaskMergeMappings.class)
				.setProgressBars(false)
				.build();
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
