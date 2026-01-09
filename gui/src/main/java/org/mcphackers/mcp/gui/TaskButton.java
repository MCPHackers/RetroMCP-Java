package org.mcphackers.mcp.gui;

import static org.mcphackers.mcp.tools.Util.enqueueRunnable;

import java.awt.event.ActionListener;

import javax.swing.*;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.mode.TaskMode;

public class TaskButton extends JButton {

	private static final long serialVersionUID = -2625827711322112358L;

	private final TaskMode linkedTask;
	private final MainGUI mcp;

	public TaskButton(MainGUI owner, TaskMode task) {
		super(task.getFullName());
		linkedTask = task;
		mcp = owner;
		addActionListener(performTask(mcp, linkedTask));
	}

	public TaskButton(MainGUI owner, TaskMode task, ActionListener defaultActionListener) {
		super(task.getFullName());
		linkedTask = task;
		mcp = owner;
		addActionListener(defaultActionListener);
	}

	public static ActionListener performTask(MCP mcp, TaskMode mode) {
		return event -> enqueueRunnable(() -> {
			if (mode.equals(TaskMode.CLEANUP) || mode.equals(TaskMode.UPDATE_MD5)) {
				String confirmMessage = mode.equals(TaskMode.CLEANUP) ? MCP.TRANSLATOR.translateKey("mcp.confirmCleanup") : MCP.TRANSLATOR.translateKey("mcp.confirmUpdateMD5");

				// This should never throw...
				MainGUI main = (MainGUI) mcp;
				int response = JOptionPane.showConfirmDialog(main.frame, confirmMessage, MCP.TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_OPTION);
				if (response == JOptionPane.YES_OPTION) {
					mcp.performTask(mode, mcp.getOptions().side);
				}
			} else {
				mcp.performTask(mode, mcp.getOptions().side);
			}
		});
	}

	public boolean getEnabled() {
		return linkedTask.isAvailable(mcp, mcp.getSide());
	}

	public void updateName() {
		setText(linkedTask.getFullName());
		setToolTipText(linkedTask.getDesc());
	}
}
