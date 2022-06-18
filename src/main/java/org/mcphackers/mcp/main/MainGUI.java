package org.mcphackers.mcp.main;

import static org.mcphackers.mcp.tools.Util.operateOnThread;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.gui.MCPFrame;
import org.mcphackers.mcp.gui.TaskButton;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;

public class MainGUI extends MCP {
	public String currentVersion;
	public Path workingDir;
	public MCPFrame frame;
	public Options options;
	public boolean isActive = true;
	
	public static final TaskMode[] TASKS = {TaskMode.DECOMPILE, TaskMode.RECOMPILE, TaskMode.REOBFUSCATE, TaskMode.BUILD, TaskMode.UPDATE_MD5, TaskMode.CREATE_PATCH};
	
	public static void main(String[] args) throws Exception {
		Path workingDir = Paths.get("");
		if(args.length >= 1) {
			try {
				workingDir = Paths.get(args[0]);
			} catch (InvalidPathException e) {}
		}
		new MainGUI(workingDir);
	}
	
	public MainGUI(Path dir) {
		workingDir = dir;
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
		isActive = active;
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
		//frame.setExtendedState(Frame.NORMAL);
		return JOptionPane.showConfirmDialog(frame, msg, title, JOptionPane.YES_NO_OPTION) == 0;
	}

	@Override
	public String inputString(String title, String msg) {
		//frame.setExtendedState(Frame.NORMAL);
		return JOptionPane.showInputDialog(frame, msg, title, JOptionPane.PLAIN_MESSAGE);
	}

	public void showMessage(String title, String msg, int type) {
		//frame.setExtendedState(Frame.NORMAL);
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
		JPanel outer = new JPanel(new BorderLayout());
		JPanel components = new JPanel();
		components.setLayout(new BoxLayout(components, BoxLayout.Y_AXIS));
		String[] lines = changelog.split("\n");
		for(String line : lines) {
			line = line.replace("`", "");
			char bullet = '\u2022';
			if(line.startsWith("# ")) {
				JLabel label = new JLabel(line.substring(2));
				label.setBorder(new EmptyBorder(0, 0, 4, 0));
				label.setFont(label.getFont().deriveFont(22F));
				components.add(label);
			}
			else if(line.startsWith("-"))
			{
				JLabel label = new JLabel(bullet + " " + line.substring(1));
				label.setFont(label.getFont().deriveFont(Font.PLAIN).deriveFont(14F));
				components.add(label);
			}
			else if(line.startsWith("  -"))
			{
				JLabel label = new JLabel(bullet + " " + line.substring(3));
				label.setFont(label.getFont().deriveFont(Font.PLAIN).deriveFont(14F));
				label.setBorder(new EmptyBorder(0, 12, 0, 0));
				components.add(label);
			}
			else {
				components.add(new JLabel(line));
			}
		}
		outer.add(components);
		JLabel label = new JLabel("Are you sure you want to update?");
		label.setFont(label.getFont().deriveFont(14F));
		label.setBorder(new EmptyBorder(10, 0, 0, 0));
		label.setHorizontalAlignment(SwingConstants.CENTER);
		outer.add(label, BorderLayout.SOUTH);
		return JOptionPane.showConfirmDialog(frame, outer, "New version found: " + version, JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) == 0;
	}

	public TaskButton getButton(TaskMode task) {
		TaskButton button;
		if(task == TaskMode.DECOMPILE) {
			ActionListener defaultActionListener = event -> operateOnThread(() -> {
				int response = 0;
				if(TaskMode.RECOMPILE.isAvailable(this, getSide())) {
					response = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete sources and decompile again?", "Confirm Action", JOptionPane.YES_NO_OPTION);
				}
				if(response == 0) {
					performTask(TaskMode.DECOMPILE, getSide());
				}
			});
			button = new TaskButton(this, task, defaultActionListener);
		}
		else if(task == TaskMode.UPDATE_MD5) {
			ActionListener defaultActionListener = event -> operateOnThread(() -> {
				int response = JOptionPane.showConfirmDialog(frame, "Are you sure you want to regenerate original hashes?", "Confirm Action", JOptionPane.YES_NO_OPTION);
				if(response == 0) {
					performTask(task, getSide());
				}
			});
			button = new TaskButton(this, task, defaultActionListener);
		}
		else {
			button = new TaskButton(this, task);
		}
		button.setPreferredSize(new Dimension(110, 30));
		button.setEnabled(false);
		return button;
	}
}
