package org.mcphackers.mcp.test;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.Task;

public class TaskTest extends Task {

	public TaskTest(MCP instance) {
		super(Side.ANY, instance);
	}

	@Override
	public void doTask() throws Exception {
		mcp.showMessage("Test", "This is a test", WARNING);
	}

}
