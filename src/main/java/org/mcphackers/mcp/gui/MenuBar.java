package org.mcphackers.mcp.gui;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.Util;

import static org.mcphackers.mcp.tools.Util.operateOnThread;

public class MenuBar extends JMenuBar {
	public final JMenu menuOptions = new JMenu("Options");
	public final JMenu mcpMenu = new JMenu("MCP");
	private final JMenu helpMenu = new JMenu("Help");
	private final JMenuItem[] sideItems = new JMenuItem[3];
	private final JMenuItem githubItem = new JMenuItem("Github Page");
	private final MainGUI owner;
	public Side side = Side.ANY;
	public Options options = new Options();

	public MenuBar(MainGUI mainGUI) {
		owner = mainGUI;
		this.menuOptions.setMnemonic(KeyEvent.VK_O);
		this.helpMenu.setMnemonic(KeyEvent.VK_H);
		initOptions();
		reloadSide();
		JMenuItem update = new JMenuItem("Check for updates");
		update.addActionListener(a -> {
			operateOnThread(() -> {
				owner.performTask(TaskMode.UPDATE_MCP, Side.ANY, false, false);
			});
		});
		JMenuItem[] start = new JMenuItem[2];
		String[] sides = {"client", "server"};
		for(int i = 0; i < 2; i++) {
			final int i2 = i;
			start[i] = new JMenuItem(TaskMode.START.getFullName() + " " + sides[i]);
			start[i].addActionListener(a -> {
				operateOnThread(() -> {
					owner.performTask(TaskMode.START, Task.sides.get(i2), false, false);
					reloadSide();
				});
			});
		}
		JMenuItem changeDir = new JMenuItem("Change working directory");
		changeDir.addActionListener(a -> {
			operateOnThread(() -> {
				String value = (String)JOptionPane.showInputDialog(owner.frame, "Enter a path to a directory", "Change working directory", JOptionPane.PLAIN_MESSAGE, null, null, owner.getWorkingDir().toAbsolutePath().toString());
				if(value != null) {
					Path p = Paths.get(value);
					if(Files.exists(p)) {
						owner.workingDir = p;
						owner.reloadVersionList();
						owner.updateButtonState();
					}
				}
			});
		});
		mcpMenu.add(start[0]);
		mcpMenu.add(start[1]);
		mcpMenu.add(update);
		mcpMenu.add(changeDir);
		final boolean taskMenu = true;
		if(taskMenu) {
			List<TaskMode> usedTasks = Arrays.asList(new TaskMode[] {
					TaskMode.DECOMPILE, TaskMode.RECOMPILE, TaskMode.REOBFUSCATE, TaskMode.CREATE_PATCH, TaskMode.BUILD,
					TaskMode.UPDATE_MCP, TaskMode.START, TaskMode.UPDATE_MD5, TaskMode.EXIT, TaskMode.HELP, TaskMode.SETUP
			});
			JMenu runTask = new JMenu("More tasks...");
			for(TaskMode task : TaskMode.registeredTasks) {
				if(usedTasks.contains(task)) {
					continue;
				}
				JMenuItem taskItem = new JMenuItem(task.getFullName());
				taskItem.addActionListener(a -> {
					operateOnThread(() -> {
						owner.performTask(task, side);
					});
				});
				runTask.add(taskItem);
			}
			mcpMenu.add(runTask);
		}
		add(mcpMenu);
		add(menuOptions);
		this.githubItem.addActionListener(e -> this.onGithubClicked());
		this.helpMenu.add(this.githubItem);
		add(helpMenu);
	}

	private void reloadSide() {
		for (JMenuItem sideItem : sideItems) {
			sideItem.setSelected(false);
		}
		int itemNumber = side.index;
		if(itemNumber == -1) {
			itemNumber = 2;
		}
		sideItems[itemNumber].setSelected(true);
	}
	
	private void setSide(int i) {
		int itemNumber = i;
		if(itemNumber == 2) {
			itemNumber = -1;
		}
		side = Task.sides.get(itemNumber);
		reloadSide();
		owner.updateButtonState();
	}

	private void initOptions() {
		JMenu sideMenu = new JMenu("Side");
		String[] sideNames = {Side.CLIENT.name, Side.SERVER.name, "All"};
		for(int i = 0; i < sideItems.length; i++) {
			final int i2 = i;
			sideItems[i] = new JRadioButtonMenuItem(sideNames[i]);
			sideItems[i].addActionListener(e -> setSide(i2));
			sideMenu.add(sideItems[i]);
		}
		menuOptions.add(sideMenu);
		
		String[] names = {
				TaskMode.DECOMPILE.getFullName(),
				TaskMode.RECOMPILE.getFullName(),
				TaskMode.REOBFUSCATE.getFullName(),
				TaskMode.BUILD.getFullName(),
				"Running"
				};
		TaskParameter[][] params = {
				{TaskParameter.PATCHES, TaskParameter.INDENTION_STRING, TaskParameter.IGNORED_PACKAGES},
				{TaskParameter.SOURCE_VERSION, TaskParameter.TARGET_VERSION, TaskParameter.BOOT_CLASS_PATH},
				{TaskParameter.OBFUSCATION},
				{TaskParameter.FULL_BUILD},
				{TaskParameter.RUN_BUILD, TaskParameter.RUN_ARGS}
		};
		Map<TaskParameter, JMenuItem> resetOptions = new HashMap<>();
		for(int i = 0; i < names.length; i++) {
			JMenu a = new JMenu(names[i]);
			for(TaskParameter param : params[i]) {
				JMenuItem b;
				if(param.type == Boolean.class) {
					b = new JRadioButtonMenuItem(param.desc);
					resetOptions.put(param, b);
					b.addActionListener(e -> {
						options.setParameter(param, b.isSelected());
					});
				}
				else {
					b = new JMenuItem(param.desc);
					b.addActionListener(u -> {
						String s = "Enter a value";
						if(param.type == String[].class) {
							s = "Enter a set of values\n(Separate values with comma)";
						}
						String value = (String)JOptionPane.showInputDialog(owner.frame, s, param.desc, JOptionPane.PLAIN_MESSAGE, null, null, options.getParameter(param));
						//TODO move this to a separate method so it can be used with other MCP implementations
						if(value != null) {
							try {
								int valueInt = Integer.parseInt(value);
								options.setParameter(param, valueInt);
								return;
							}
							catch (NumberFormatException e) {}
							catch (IllegalArgumentException e) {
								owner.showMessage(param.desc, "Invalid value!", Task.ERROR);
								return;
							}
							if(value.equals("true") || value.equals("false")) {
								try {
									boolean valueBoolean = Boolean.parseBoolean(value);
									options.setParameter(param, valueBoolean);
									return;
								}
								catch (IllegalArgumentException e) {
									owner.showMessage(param.desc, "Invalid value!", Task.ERROR);
									return;
								}
							}
							else {
								if(value.contains(",")) {
									try {
										String[] values = value.split(",");
										for(int i2 = 0 ; i2 < values.length; i2++) {
											values[i2] = values[i2].trim();
											values[i2] = values[i2].replace("\\n", "\n").replace("\\t", "\t");
										}
										options.setParameter(param, values);
										return;
									}
									catch (IllegalArgumentException e) {
										owner.showMessage(param.desc, "Invalid value!", Task.ERROR);
										return;
									}
								}
								else {
									try {
										value = value.replace("\\n", "\n").replace("\\t", "\t");
										options.setParameter(param, value);
										return;
									}
									catch (IllegalArgumentException e) {
										owner.showMessage(param.desc, "Invalid value!", Task.ERROR);
										return;
									}
								}
							}
						}
					});
				}
				a.add(b);
			}
			menuOptions.add(a);
		}
		resetDefaults(resetOptions);
		JMenuItem reset = new JMenuItem("Reset to defaults");
		reset.addActionListener(e -> {
			resetDefaults(resetOptions);
		});
		menuOptions.add(reset);
	}
	
	private void resetDefaults(Map<TaskParameter, JMenuItem> resetOptions) {
		options.resetDefaults();
		for(Map.Entry<TaskParameter, JMenuItem> entry : resetOptions.entrySet()) {
			entry.getValue().setSelected(options.getBooleanParameter(entry.getKey()));
		}
	}

	private void onGithubClicked() {
		Util.openUrl("https://github.com/MCPHackers/RetroMCP-Java");
	}
}
