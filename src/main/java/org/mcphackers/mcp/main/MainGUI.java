package org.mcphackers.mcp.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JOptionPane;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.gui.MCPFrame;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;


public class MainGUI extends MCP {
	public String currentVersion;
	public Path workingDir;
	public MCPFrame frame;
	public Side side = Side.ANY;
	public Options options = new Options();
	
	public static void main(String[] args) throws Exception {
		new MainGUI();
	}
	
	public MainGUI() {
		workingDir = Paths.get("");
		JavaCompiler c = ToolProvider.getSystemJavaCompiler();
		if (c == null) {
			JOptionPane.showMessageDialog(null, "Java Development Kit not found!", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		frame = new MCPFrame(this);
	}
	
	@Override
	public void setProgressBars(List<Task> tasks, TaskMode mode) {
		frame.setProgressBars(tasks, mode);
	}

	@Override
	public void clearProgressBars() {
		frame.resetProgressBars();
	}

	public void setActive(boolean active) {
		if(active) {
			frame.updateButtonState();
		}
		else {
			frame.setAllButtonsInactive();
		}
	}

	@Override
	public String getCurrentVersion() {
		return currentVersion;
	}

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

	@Override
	public Options getOptions() {
		return options;
	}

	@Override
	public void setProgress(int side, String progressMessage) {
		frame.setProgress(side, progressMessage);
	}

	@Override
	public void setProgress(int side, int progress) {
		frame.setProgress(side, progress);
	}

	@Override
	public boolean yesNoInput(String title, String msg) {
		return JOptionPane.showConfirmDialog(frame, msg, title, JOptionPane.YES_NO_OPTION) == 0;
	}

	@Override
	public String inputString(String title, String msg) {
		return JOptionPane.showInputDialog(frame, msg, title, JOptionPane.PLAIN_MESSAGE);
	}

	public void showMessage(String title, String msg, int type) {
		switch (type) {
		case Task.INFO:
			type = JOptionPane.INFORMATION_MESSAGE;
			break;
		case Task.WARNING:
			type = JOptionPane.WARNING_MESSAGE;
			break;
		case Task.ERROR:
			type = JOptionPane.ERROR_MESSAGE;
			break;
		}
		JOptionPane.showMessageDialog(frame, msg, title, type);
	}

	@Override
	public void setCurrentVersion(String version) {
		currentVersion = version;
		frame.setCurrentVersion(version);
	}

	@Override
	public Path getWorkingDir() {
		return workingDir;
	}
}
