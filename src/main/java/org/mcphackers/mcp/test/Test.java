package org.mcphackers.mcp.test;

import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.TaskParameter;

public class Test extends MainGUI {
	public static final TaskMode TEST = new TaskMode("test", "Test", TaskTest.class, new TaskParameter[] {});
}
