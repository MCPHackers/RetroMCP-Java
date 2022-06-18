package org.mcphackers.mcp.gui;

import static org.mcphackers.mcp.tools.Util.operateOnThread;

import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.mode.TaskMode;

public class TaskButton extends JButton {
	private TaskMode linkedTask;
	private MCP mcp;
	
	public TaskButton(MCP owner, TaskMode task) {
		super(task.getFullName());
		linkedTask = task;
		mcp = owner;
		addActionListener(performTask(mcp, linkedTask));
	}

	public TaskButton(MCP owner, TaskMode task, ActionListener defaultActionListener) {
		super(task.getFullName());
		linkedTask = task;
		mcp = owner;
		addActionListener(defaultActionListener);
	}
	
	public static ActionListener performTask(MCP mcp, TaskMode mode) {
		return event -> operateOnThread(() -> mcp.performTask(mode, mcp.getOptions().side));
	}

	public boolean getEnabled() {
		return linkedTask.isAvailable(mcp, mcp.getOptions().side);
	}
}
