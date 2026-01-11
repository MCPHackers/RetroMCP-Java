package org.mcphackers.mcp.main;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.Theme;
import org.mcphackers.mcp.gui.MCPFrame;
import org.mcphackers.mcp.gui.TaskButton;
import org.mcphackers.mcp.gui.TextAreaContextMenu;
import org.mcphackers.mcp.gui.TextAreaOutputStream;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;
import org.mcphackers.mcp.tools.versions.json.Version;

/**
 * GUI implementation of MCP
 */
public class MainGUI extends MCP {
	public static final TaskMode[] TASKS = {TaskMode.DECOMPILE, TaskMode.RECOMPILE, TaskMode.REOBFUSCATE, TaskMode.BUILD, TaskMode.CREATE_PATCH};
	public static final String[] TABS = {"task.decompile", "task.recompile", "task.reobfuscate", "task.build", "options.running"};
	public static final TaskParameter[][] TAB_PARAMETERS = {
			{TaskParameter.PATCHES, TaskParameter.FERNFLOWER_OPTIONS, TaskParameter.IGNORED_PACKAGES, TaskParameter.OUTPUT_SRC, TaskParameter.DECOMPILE_RESOURCES, TaskParameter.GUESS_GENERICS, TaskParameter.STRIP_GENERICS},
			{TaskParameter.SOURCE_VERSION, TaskParameter.TARGET_VERSION, TaskParameter.JAVA_HOME, TaskParameter.JAVAC_ARGS}, {TaskParameter.OBFUSCATION, TaskParameter.SRG_OBFUSCATION, TaskParameter.EXCLUDED_CLASSES, TaskParameter.STRIP_SOURCE_FILE},
			{TaskParameter.FULL_BUILD}, {TaskParameter.RUN_BUILD, TaskParameter.RUN_ARGS, TaskParameter.GAME_ARGS}
	};
	public Theme theme = Theme.THEMES_MAP.get(UIManager.getCrossPlatformLookAndFeelClassName());
	public MCPFrame frame;
	public boolean isActive = true;
	public Version currentVersion;
	public JTextPane textPane;

	public MainGUI() {
		this.initialize();
	}

	public MainGUI(Path dir) {
		this.options.workingDir = dir;
		this.initialize();
	}

	private void initialize() {
		isGUI = true;
		if (!theme.themeClass.equals(options.theme) && options.theme != null) {
			theme = Theme.THEMES_MAP.get(options.theme);
		}
		changeTheme(theme);
		textPane = new JTextPane();
		PrintStream origOut = System.out;
		PrintStream interceptor = new TextAreaOutputStream(textPane, origOut);
		System.setOut(interceptor);
		origOut = System.err;
		interceptor = new TextAreaOutputStream(textPane, origOut);
		System.setErr(interceptor);
		this.textPane.setComponentPopupMenu(new TextAreaContextMenu(this));

		if (options.lang != null) {
			changeLanguage(options.lang);
		}
		JavaCompiler c = ToolProvider.getSystemJavaCompiler();
		if (c == null) {
			JOptionPane.showMessageDialog(null, MCP.TRANSLATOR.translateKey("mcp.needJDK"), MCP.TRANSLATOR.translateKey("mcp.error"), JOptionPane.ERROR_MESSAGE);
		}
		Path versionPath = MCPPaths.get(this, MCPPaths.VERSION);
		if (Files.exists(versionPath)) {
			try {
				currentVersion = Version.from(new JSONObject(new String(Files.readAllBytes(versionPath))));
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}
		}
		if (!VersionParser.mappingsJson.equals(VersionParser.DEFAULT_JSON)) {
			warning("Using old or third party manifest URL: " + VersionParser.mappingsJson);
			warning("If this is not intentional, please update versionUrl in options.cfg to " + VersionParser.DEFAULT_JSON);
		}
		frame = new MCPFrame(this);
		if (Util.getJavaVersion() > 8) {
			warning("JDK " + Util.getJavaVersion() + " is being used! Java 8 is recommended.");
		}
	}

	public static void main(String[] args) {
		if (args.length >= 1) {
			try {
				Path workingDir = Paths.get(args[0]);
				new MainGUI(workingDir);
			} catch (InvalidPathException ignored) {
			}
		} else {
			new MainGUI();
		}
	}

	@Override
	public void setProgressBars(List<Task> tasks, TaskMode mode) {
		frame.setProgressBars(tasks, mode);
	}

	@Override
	public void clearProgressBars() {
		frame.resetProgressBars();
	}

	@Override
	public void setActive(boolean active) {
		isActive = active;
		if (active) {
			frame.updateButtonState();
		} else {
			frame.setAllButtonsInactive();
		}
	}

	@Override
	public Version getCurrentVersion() {
		return currentVersion;
	}

	@Override
	public void setCurrentVersion(Version version) {
		currentVersion = version;
		frame.setCurrentVersion(version == null ? null : this.getVersionParser().getVersion(version.id));
	}

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

	@Override
	public void warning(String msg) {
		System.out.println("WARNING: " + msg);
	}

	@Override
	public void error(String msg) {
		System.out.println("ERROR: " + msg);
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
		frame.setState(Frame.NORMAL);
		return JOptionPane.showConfirmDialog(frame, msg, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	@Override
	public String inputString(String title, String msg) {
		frame.setState(Frame.NORMAL);
		return JOptionPane.showInputDialog(frame, msg, title, JOptionPane.PLAIN_MESSAGE);
	}

	@SuppressWarnings("MagicConstant")
	@Override
	public void showMessage(String title, String msg, int type) {
		frame.setState(Frame.NORMAL);
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
	public void showMessage(String title, String msg, Throwable e) {
		JPanel panel = new JPanel(new BorderLayout());
		JTextArea text = new JTextArea();
		text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		text.append(sw.toString());
		panel.add(new JLabel(msg), BorderLayout.NORTH);
		panel.add(new JScrollPane(text), BorderLayout.CENTER);
		JOptionPane.showMessageDialog(frame, panel, title, JOptionPane.ERROR_MESSAGE);
	}

	public void exit() {
		if (!isActive) {
			if (!yesNoInput(MCP.TRANSLATOR.translateKey("mcp.confirmAction"), MCP.TRANSLATOR.translateKey("mcp.confirmExit")))
				return;
		}
		frame.dispose();
		System.exit(0);
	}

	@Override
	public boolean updateDialogue(String changelog, String version) {
		JPanel outer = new JPanel(new BorderLayout());
		JPanel components = new JPanel();
		components.setLayout(new BoxLayout(components, BoxLayout.Y_AXIS));
		String[] lines = changelog.split("\n");
		for (String line : lines) {
			line = line.replace("`", "");
			char bullet = '\u2022';
			if (line.startsWith("# ")) {
				JLabel label = new JLabel(line.substring(2));
				label.setBorder(new EmptyBorder(0, 0, 4, 0));
				label.setFont(label.getFont().deriveFont(22F));
				components.add(label);
			} else if (line.startsWith("-")) {
				JLabel label = new JLabel(bullet + " " + line.substring(1));
				label.setFont(label.getFont().deriveFont(Font.PLAIN).deriveFont(14F));
				components.add(label);
			} else if (line.startsWith("  -")) {
				JLabel label = new JLabel(bullet + " " + line.substring(3));
				label.setFont(label.getFont().deriveFont(Font.PLAIN).deriveFont(14F));
				label.setBorder(new EmptyBorder(0, 12, 0, 0));
				components.add(label);
			} else {
				components.add(new JLabel(line));
			}
		}
		outer.add(components);
		JLabel label = new JLabel(MCP.TRANSLATOR.translateKey("mcp.confirmUpdate"));
		label.setFont(label.getFont().deriveFont(14F));
		label.setBorder(new EmptyBorder(10, 0, 0, 0));
		label.setHorizontalAlignment(SwingConstants.CENTER);
		outer.add(label, BorderLayout.SOUTH);
		return JOptionPane.showConfirmDialog(frame, outer, MCP.TRANSLATOR.translateKey("mcp.newVersion") + " " + version, JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) == 0;
	}

	public void changeWorkingDirectory() {
		JFileChooser f = new JFileChooser(getWorkingDir().toAbsolutePath().toFile());
		f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (f.showDialog(this.frame, MCP.TRANSLATOR.translateKey("mcp.selectDir")) == JFileChooser.APPROVE_OPTION) {
			File file = f.getSelectedFile();
			Path p = file.toPath();
			if (Files.isDirectory(p)) {
				this.options = new Options(this, Paths.get("options.cfg"));
				this.options.workingDir = p;
				this.options.save();
				VersionParser versionParser = this.getVersionParser();
				Path versionPath = MCPPaths.get(this, MCPPaths.VERSION);
				if (Files.exists(versionPath)) {
					try {
						currentVersion = Version.from(new JSONObject(new String(Files.readAllBytes(versionPath))));
					} catch (JSONException | IOException e) {
						e.printStackTrace();
					}
				}
				this.frame.setCurrentVersion(this.currentVersion == null ? null : versionParser.getVersion(this.currentVersion.id));
				this.frame.reloadText();
				this.frame.reloadVersionList();
				this.frame.updateButtonState();
				this.frame.menuBar.reloadOptions();
				this.frame.menuBar.reloadSide();
			}
		}
	}

	public void inputOptionsValue(TaskParameter param) {
		String s = MCP.TRANSLATOR.translateKey("options.enterValue");
		if (param.type == String[].class) {
			s = MCP.TRANSLATOR.translateKey("options.enterValues") + "\n" + MCP.TRANSLATOR.translateKey("options.enterValues.info");
		}
		String value = (String) JOptionPane.showInputDialog(frame, s, param.getDesc(), JOptionPane.PLAIN_MESSAGE, null, null, Util.convertToEscapedString(String.valueOf(options.getParameter(param))));
		safeSetParameter(param, value);
		options.save();
	}

	public void setupVersion(VersionData versionData) {
		VersionParser versionParser = this.getVersionParser();
		Version version = getCurrentVersion();
		if (versionData != null && !versionData.equals(version == null ? null : versionParser.getVersion(version.id))) {
			int response = JOptionPane.showConfirmDialog(frame, MCP.TRANSLATOR.translateKey("mcp.confirmSetup"), MCP.TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_OPTION);
			if (response == JOptionPane.YES_OPTION) {
				setParameter(TaskParameter.SETUP_VERSION, versionData.id);
				performTask(TaskMode.SETUP, Side.ANY);
			} else {
				frame.setCurrentVersion(versionParser.getVersion(version == null ? null : version.id));
			}
		}
	}

	public TaskButton getButton(TaskMode task) {
		TaskButton button;
		if (task == TaskMode.DECOMPILE) {
			ActionListener defaultActionListener = event -> Util.enqueueRunnable(() -> {
				int response = JOptionPane.YES_OPTION;
				if (TaskMode.RECOMPILE.isAvailable(this, getSide())) {
					response = JOptionPane.showConfirmDialog(frame, MCP.TRANSLATOR.translateKey("mcp.confirmDecompile"), MCP.TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_OPTION);
					if (response == JOptionPane.YES_OPTION) {
						int response2 = JOptionPane.showConfirmDialog(frame, MCP.TRANSLATOR.translateKey("mcp.askSourceBackup"), MCP.TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_CANCEL_OPTION);
						if (response2 == JOptionPane.YES_OPTION) {
							performTask(TaskMode.BACKUP_SRC, getSide(), false);
						} else if (response2 != JOptionPane.NO_OPTION) {
							response = response2;
						}
					}
				}
				if (response == JOptionPane.YES_OPTION) {
					performTask(TaskMode.DECOMPILE, getSide());
				}
			});
			button = new TaskButton(this, task, defaultActionListener);
		} else {
			button = new TaskButton(this, task);
		}
		button.setEnabled(false);
		return button;
	}

	public void setSide(Side side) {
		getOptions().side = side;
		getOptions().save();
		frame.updateButtonState();
		frame.menuBar.reloadSide();
	}

	public final void changeTheme(Theme theme) {
		try {
			if (theme != null) {
				UIManager.setLookAndFeel(theme.themeClass);
				JFrame frame = this.frame;
				if (frame != null) {
					SwingUtilities.updateComponentTreeUI(frame);
				}
				this.theme = theme;
				this.options.theme = theme.themeClass;
			}
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException |
				 IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
