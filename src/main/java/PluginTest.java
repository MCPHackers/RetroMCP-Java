import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.plugin.MCPPlugin;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskDecompile;

public class PluginTest implements MCPPlugin {

	@Override
	public String pluginId() {
		return "testPlugin";
	}

	@Override
	public void init() {
		
	}

	@Override
	public void onTaskEvent(TaskEvent event, Task task) {
		if(task instanceof TaskDecompile) {
			if(event == TaskEvent.PRE_TASK) {
				task.log("PRE_TASK event triggered inside of decompile task");
			}
			if(event == TaskEvent.POST_TASK) {
				task.log("POST_TASK event triggered inside of decompile task");
			}
		}
		else {
			if(event == TaskEvent.PRE_TASK) {
				task.log("PRE_TASK event triggered");
			}
			if(event == TaskEvent.POST_TASK) {
				task.log("POST_TASK event triggered");
			}
		}
	}

	@Override
	public void onMCPEvent(MCPEvent event, MCP mcp) {
		if(event == MCPEvent.STARTED_TASKS) {
			mcp.log("Running tasks");
		}
	}
}
