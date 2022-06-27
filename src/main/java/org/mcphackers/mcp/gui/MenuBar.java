package org.mcphackers.mcp.gui;

import static org.mcphackers.mcp.tools.Util.operateOnThread;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import org.mcphackers.mcp.Language;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.Os;
import org.mcphackers.mcp.tools.Util;

public class MenuBar extends JMenuBar {
	public final JMenu menuOptions = new JMenu();
	public final JMenu mcpMenu = new JMenu("MCP");
	public final List<JMenuItem> togglableComponents = new ArrayList<>();
	public final Map<JMenuItem, String> translatableComponents = new HashMap<>();
	public final Map<Side, JMenuItem> start = new HashMap<>();
	public final Map<TaskMode, JMenuItem> taskItems = new HashMap<>();
	public final Map<TaskParameter, JMenuItem> optionItems = new HashMap<>();
	private final JMenu helpMenu = new JMenu();
	private JMenuItem[] sideItems;
	private final MCPFrame owner;
	private MainGUI mcp;

	public MenuBar(MCPFrame frame) {
		owner = frame;
		mcp = frame.mcp;
		this.menuOptions.setMnemonic(KeyEvent.VK_O);
		this.helpMenu.setMnemonic(KeyEvent.VK_H);
		this.mcpMenu.setMnemonic(KeyEvent.VK_M);
		translatableComponents.put(menuOptions, "options");
		translatableComponents.put(helpMenu, "task.help");
		initOptions();
		JMenuItem update = new JMenuItem();
		translatableComponents.put(update, "mcp.checkUpdate");
		update.addActionListener(a -> operateOnThread(() -> mcp.performTask(TaskMode.UPDATE_MCP, Side.ANY, false)));
		Side[] sides = {Side.CLIENT, Side.SERVER};
		for(Side side : sides) {
			JMenuItem start = new JMenuItem();
			translatableComponents.put(start, side == Side.CLIENT ? "mcp.startClient" : "mcp.startServer");
			togglableComponents.add(start);
			start.addActionListener(a -> {
				operateOnThread(() -> {
					mcp.performTask(TaskMode.START, side, false);
					reloadSide();
				});
			});
			mcpMenu.add(start);
			this.start.put(side, start);
		}
		reloadSide();
		JMenuItem browseDir = new JMenuItem();
		translatableComponents.put(browseDir, "mcp.viewDir");
		browseDir.addActionListener(a -> {
			try {
				if (Os.getOs() == Os.LINUX) {
					Util.openUrl(mcp.getWorkingDir().toAbsolutePath().toUri().toString()); // Call to xdg-open
				} else {
					Desktop.getDesktop().open(mcp.getWorkingDir().toAbsolutePath().toFile());
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		JMenuItem changeDir = new JMenuItem();
		translatableComponents.put(changeDir, "mcp.changeDir");
		changeDir.addActionListener(a -> operateOnThread(() -> {
				String value = (String)JOptionPane.showInputDialog(owner, MCP.TRANSLATOR.translateKey("mcp.enterDir"), MCP.TRANSLATOR.translateKey("mcp.changeDir"), JOptionPane.PLAIN_MESSAGE, null, null, mcp.getWorkingDir().toAbsolutePath().toString());
				if(value != null) {
					Path p = Paths.get(value);
					if(Files.exists(p)) {
						mcp.workingDir = p;
						mcp.options = new Options(MCPPaths.get(mcp, "options.cfg"));
						reloadOptions();
						reloadSide();
						mcp.options.save();
						owner.reloadVersionList();
						owner.updateButtonState();
					}
				}
			})
		);
		mcpMenu.add(update);
		mcpMenu.add(browseDir);
		mcpMenu.add(changeDir);
		final boolean taskMenu = true;
		if(taskMenu) {
			List<TaskMode> usedTasks = new ArrayList<>();
			usedTasks.addAll(Arrays.asList(MainGUI.TASKS));
			usedTasks.addAll(Arrays.asList(TaskMode.UPDATE_MCP, TaskMode.START, TaskMode.EXIT, TaskMode.HELP, TaskMode.SETUP));
			JMenu moreTasks = new JMenu();
			translatableComponents.put(moreTasks, "mcp.moreTasks");
			togglableComponents.add(moreTasks);
			for(TaskMode task : TaskMode.registeredTasks) {
				if(usedTasks.contains(task)) {
					continue;
				}
				JMenuItem taskItem = new JMenuItem();
				taskItems.put(task, taskItem);
				taskItem.addActionListener(TaskButton.performTask(mcp, task));
				moreTasks.add(taskItem);
			}
			mcpMenu.add(moreTasks);
		}
		JMenuItem exit = new JMenuItem();
		translatableComponents.put(exit, "task.exit");
		exit.addActionListener(a -> System.exit(0));
		mcpMenu.add(exit);
		togglableComponents.add(update);
		togglableComponents.add(changeDir);
		add(mcpMenu);
		add(menuOptions);
		JMenuItem githubItem = new JMenuItem();
		JMenuItem wiki = new JMenuItem();
		translatableComponents.put(githubItem, "mcp.github");
		translatableComponents.put(wiki, "mcp.wiki");
		githubItem.addActionListener(e -> Util.openUrl(MCP.githubURL));
		wiki.addActionListener(e -> Util.openUrl(MCP.githubURL + "/wiki"));
		this.helpMenu.add(githubItem);
		this.helpMenu.add(wiki);
		add(helpMenu);
		JMenu langMenu = new JMenu();
		translatableComponents.put(langMenu, "options.language");
		for(Language lang : Language.values()) {
			JMenuItem langItem = new JMenuItem(MCP.TRANSLATOR.getLangName(lang));
			langItem.addActionListener(a -> {
				mcp.changeLanguage(lang);
				owner.reloadText();
			});
			langMenu.add(langItem);
		}
		add(langMenu);
	}

	private void reloadSide() {
		for (JMenuItem sideItem : sideItems) {
			if(sideItem != null) {
				sideItem.setSelected(false);
			}
		}
		int itemNumber = mcp.getSide().index;
		if(itemNumber < 0) {
			itemNumber = sideItems.length - 1;
		}
		sideItems[itemNumber].setSelected(true);
	}
	
	private void setSide(Side side) {
		mcp.getOptions().side = side;
		mcp.getOptions().save();
		reloadSide();
		owner.updateButtonState();
	}

	private void initOptions() {
		JMenu sideMenu = new JMenu();
		translatableComponents.put(sideMenu, "mcp.side");
		sideItems = new JMenuItem[Side.values().length];
		for(Side side : Side.values()) {
			final int i = side.index;
			if(i >= 0) {
				sideItems[i] = new JRadioButtonMenuItem(side.getName());
				sideItems[i].addActionListener(e -> setSide(side));
				sideMenu.add(sideItems[i]);
			}
		}
		sideItems[sideItems.length - 1] = new JRadioButtonMenuItem(Side.ANY.getName());
		sideItems[sideItems.length - 1].addActionListener(e -> setSide(Side.ANY));
		sideMenu.add(sideItems[sideItems.length - 1]);
		menuOptions.add(sideMenu);
		
		String[] names = {
				"task.decompile",
				"task.recompile",
				"task.reobfuscate",
				"task.build",
				"options.running"
				};
		TaskParameter[][] params = {
				{TaskParameter.PATCHES, TaskParameter.DECOMPILE_OVERRIDE, TaskParameter.INDENTATION_STRING, TaskParameter.IGNORED_PACKAGES},
				{TaskParameter.SOURCE_VERSION, TaskParameter.TARGET_VERSION, TaskParameter.JAVA_HOME},
				{TaskParameter.OBFUSCATION},
				{TaskParameter.FULL_BUILD},
				{TaskParameter.RUN_BUILD, TaskParameter.RUN_ARGS}
		};
		for(int i = 0; i < names.length; i++) {
			JMenu a = new JMenu();
			translatableComponents.put(a, names[i]);
			for(TaskParameter param : params[i]) {
				JMenuItem b;
				if(param.type == Boolean.class) {
					b = new JRadioButtonMenuItem();
					translatableComponents.put(b, "task.param." + param.name);
					optionItems.put(param, b);
					b.addActionListener(e -> mcp.options.setParameter(param, b.isSelected()));
				}
				else {
					b = new JMenuItem(param.getDesc());
					b.addActionListener(u -> {
						String s = MCP.TRANSLATOR.translateKey("options.enterValue");
						if(param.type == String[].class) {
							s = MCP.TRANSLATOR.translateKey("options.enterValues") + "\n" + MCP.TRANSLATOR.translateKey("options.enterValues.info");
						}
						String value = (String)JOptionPane.showInputDialog(owner, s, param.getDesc(), JOptionPane.PLAIN_MESSAGE, null, null, Util.convertToEscapedString(String.valueOf(mcp.options.getParameter(param))));
						mcp.safeSetParameter(param, value);
						
					});
				}
				a.add(b);
			}
			menuOptions.add(a);
		}
		reloadOptions();
		JMenuItem reset = new JMenuItem();
		translatableComponents.put(reset, "options.resetDefaults");
		reset.addActionListener(e -> {
			mcp.getOptions().resetDefaults();
			reloadOptions();
			reloadSide();
			owner.updateButtonState();
		});
		menuOptions.add(reset);
	}
	
	private void reloadOptions() {
		for(Map.Entry<TaskParameter, JMenuItem> entry : optionItems.entrySet()) {
			entry.getValue().setSelected(mcp.options.getBooleanParameter(entry.getKey()));
		}
	}

	public void setComponentsEnabled(boolean b) {
		for(JMenuItem item : togglableComponents) {
			item.setEnabled(b);
		}
	}

	public void reloadText() {
		for(Entry<JMenuItem, String> entry : translatableComponents.entrySet()) {
			entry.getKey().setText(MCP.TRANSLATOR.translateKey(entry.getValue()));
		}
		for(Entry<TaskMode, JMenuItem> entry : taskItems.entrySet()) {
			entry.getValue().setText(entry.getKey().getFullName());
		}
	}
}
