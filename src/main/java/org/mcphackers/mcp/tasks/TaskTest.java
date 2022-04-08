package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;

public class TaskTest extends Task {

	public TaskTest(MCP instance) {
		super(Side.ANY, instance);
	}

	@Override
	public void doTask() throws Exception {
		step();
		log(mcp.inputString("Test", "Input something"));
		setProgress("Input accepted", 50);
		mcp.showMessage("Test", "Something happened!", INFO);
	}

}
