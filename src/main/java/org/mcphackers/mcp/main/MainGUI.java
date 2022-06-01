package org.mcphackers.mcp.main;

import java.awt.Font;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.gui.MCPFrame;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;


public class MainGUI extends MCP {
	public String currentVersion;
	public Path workingDir;
	public MCPFrame frame;
	public Options options;
	
	public static final TaskMode[] TASKS = {TaskMode.DECOMPILE, TaskMode.RECOMPILE, TaskMode.REOBFUSCATE, TaskMode.BUILD, TaskMode.UPDATE_MD5, TaskMode.CREATE_PATCH};
	
	public static void main(String[] args) throws Exception {
		SwingUtilities.invokeLater(() -> new MainGUI());
	}
	
	public MainGUI() {
		workingDir = Paths.get("");
		options = new Options(MCPPaths.get(this, "options.cfg"));
		JavaCompiler c = ToolProvider.getSystemJavaCompiler();
		if (c == null) {
			JOptionPane.showMessageDialog(null, "Java Development Kit is required to recompile!", "Error", JOptionPane.ERROR_MESSAGE);
		}
		frame = new MCPFrame(this);
	}
	
	public Side getSide() {
		return getOptions().side;
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

	@Override
	public boolean updateDialogue(String changelog, String version) {
		JPanel components = new JPanel();
		components.setLayout(new BoxLayout(components, BoxLayout.Y_AXIS));
		String[] lines = changelog.split("\n");
		for(String line : lines) {
			line = line.replace("`", "");
			if(line.startsWith("# ")) {
				JLabel label = new JLabel(line.substring(2));
				Font font = label.getFont();
				font.deriveFont(3F);
				label.setFont(new Font(font.getName(), Font.PLAIN, 18));
				components.add(label);
			}
			else if(line.startsWith("-"))
			{
				String bullet = "•";
				JLabel label = new JLabel(bullet + line.substring(1));
				components.add(label);
			}
			else {
				components.add(new JLabel(line));
			}
		}
		components.add(new JLabel("Are you sure you want to update?"));
		return JOptionPane.showConfirmDialog(frame, components, "New version found: " + version, JOptionPane.YES_NO_OPTION) == 0;
	}
}
