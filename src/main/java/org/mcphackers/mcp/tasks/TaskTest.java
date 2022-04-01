package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;

public class TaskTest extends Task {

	public TaskTest(MCP instance) {
		super(Side.NONE, instance);
	}

	@Override
	public void doTask() throws Exception {
		step();
		log(mcp.inputString(getName(), "Input something"));
		setProgress("Input accepted", 50);
		mcp.showPopup(getName(), "Something happened!", INFO);
	}

	@Override
	public String getName() {
		return "Test";
	}

}
