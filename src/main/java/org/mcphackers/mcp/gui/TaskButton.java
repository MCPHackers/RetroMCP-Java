package org.mcphackers.mcp.gui;

import static org.mcphackers.mcp.tools.Util.operateOnThread;

import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.mode.TaskMode;

public class TaskButton extends JButton {
	private TaskMode linkedTask;
	private MCPFrame frame;
	
	public TaskButton(MCPFrame owner, TaskMode task) {
		super(task.getFullName());
		linkedTask = task;
		frame = owner;
		addActionListener(performTask(frame.mcp, linkedTask));
	}

	public TaskButton(MCPFrame owner, TaskMode task, ActionListener defaultActionListener) {
		super(task.getFullName());
		linkedTask = task;
		frame = owner;
		addActionListener(defaultActionListener);
	}
	
	public static ActionListener performTask(MainGUI mcp, TaskMode mode) {
		return event -> operateOnThread(() -> mcp.performTask(mode, mcp.getOptions().side));
	}

	public boolean getEnabled() {
		return linkedTask.isAvailable(frame.mcp, frame.mcp.getOptions().side);
	}
}
