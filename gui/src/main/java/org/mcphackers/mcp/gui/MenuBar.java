package org.mcphackers.mcp.gui;

import static org.mcphackers.mcp.tools.Util.enqueueRunnable;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.*;

import org.mcphackers.mcp.Language;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.Theme;
import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.OS;
import org.mcphackers.mcp.tools.Util;

public class MenuBar extends JMenuBar {
	private static final long serialVersionUID = 5993064672172544233L;

	public final JMenu menuOptions = new JMenu();
	public final JMenu mcpMenu = new JMenu("MCP");
	public final List<JMenuItem> togglableComponents = new ArrayList<>();
	public final Map<JMenuItem, String> translatableComponents = new HashMap<>();
	public final Map<Side, JMenuItem> start = new HashMap<>();
	public final Map<TaskMode, JMenuItem> taskItems = new HashMap<>();
	public final Map<TaskParameter, JMenuItem> optionItems = new HashMap<>();
	private final MCPFrame owner;
	private final MainGUI mcp;
	private final JMenuItem[] themeItems;
	private final JMenuItem[] langItems;
	private JMenuItem[] sideItems;

	protected MenuBar(MCPFrame frame) {
		owner = frame;
		mcp = frame.mcp;
		JMenu helpMenu = new JMenu();
		helpMenu.setMnemonic(KeyEvent.VK_H);
		this.mcpMenu.setMnemonic(KeyEvent.VK_M);
		translatableComponents.put(menuOptions, "options");
		translatableComponents.put(helpMenu, "task.help");
		initOptions();
		JMenuItem update = new JMenuItem();
		translatableComponents.put(update, "mcp.checkUpdate");
		update.addActionListener(a -> enqueueRunnable(() -> mcp.performTask(TaskMode.UPDATE_MCP, Side.ANY, false)));
		Side[] sides = {Side.CLIENT, Side.SERVER};
		for (Side side : sides) {
			JMenuItem start = new JMenuItem();
			translatableComponents.put(start, side == Side.CLIENT ? "mcp.startClient" : "mcp.startServer");
			togglableComponents.add(start);
			start.addActionListener(a -> enqueueRunnable(() -> {
				mcp.performTask(TaskMode.START, side, false);
				reloadSide();
			}));
			mcpMenu.add(start);
			this.start.put(side, start);
		}
		reloadSide();
		JMenuItem browseDir = new JMenuItem();
		translatableComponents.put(browseDir, "mcp.viewDir");
		browseDir.addActionListener(a -> {
			try {
				if (OS.getOs() == OS.linux) {
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
		changeDir.addActionListener(a -> enqueueRunnable(() -> {
					mcp.changeWorkingDirectory();
					reloadOptions();
					reloadSide();
				})
		);
		mcpMenu.add(update);
		mcpMenu.add(browseDir);
		mcpMenu.add(changeDir);
		List<TaskMode> usedTasks = new ArrayList<>();
		usedTasks.addAll(Arrays.asList(MainGUI.TASKS));
		usedTasks.addAll(Arrays.asList(TaskMode.UPDATE_MCP, TaskMode.START, TaskMode.EXIT, TaskMode.HELP, TaskMode.SETUP));
		JMenu moreTasks = new JMenu();
		translatableComponents.put(moreTasks, "mcp.moreTasks");
		togglableComponents.add(moreTasks);
		for (TaskMode task : TaskMode.registeredTasks) {
			if (usedTasks.contains(task)) {
				continue;
			}
			JMenuItem taskItem = new JMenuItem();
			taskItems.put(task, taskItem);
			taskItem.addActionListener(TaskButton.performTask(mcp, task));
			moreTasks.add(taskItem);
		}
		mcpMenu.add(moreTasks);
		JMenuItem exit = new JMenuItem();
		translatableComponents.put(exit, "task.exit");
		exit.addActionListener(a -> mcp.exit());
		mcpMenu.add(exit);
		togglableComponents.add(update);
		togglableComponents.add(changeDir);
		add(mcpMenu);
		add(menuOptions);
		JMenu langMenu = new JMenu();
		translatableComponents.put(langMenu, "options.language");
		int i = 0;
		langItems = new JMenuItem[Language.values().length];
		for (Language lang : Language.values()) {
			JMenuItem langItem = new JRadioButtonMenuItem(MCP.TRANSLATOR.getLangName(lang));
			langItem.addActionListener(a -> {
				mcp.changeLanguage(lang);
				owner.reloadText();
				for (JMenuItem item : langItems) {
					item.setSelected(false);
				}
				langItem.setSelected(true);
				mcp.options.save();
			});
			if (lang.equals(MCP.TRANSLATOR.currentLang)) {
				langItem.setSelected(true);
			}
			langItems[i] = langItem;
			langMenu.add(langItem);
			i++;
		}
		add(langMenu);

		JMenu themeMenu = new JMenu();
		translatableComponents.put(themeMenu, "options.theme");
		themeItems = new JMenuItem[Theme.THEMES.size()];
		i = 0;
		for (Theme theme : Theme.THEMES) {
			JMenuItem themeItem = new JRadioButtonMenuItem(theme.themeName);
			themeItem.addActionListener((actionEvent) -> {
				mcp.changeTheme(theme);
				owner.reloadText();
				for (JMenuItem item : themeItems) {
					item.setSelected(false);
				}
				themeItem.setSelected(true);
				mcp.options.save();
			});
			if (theme.equals(this.mcp.theme)) {
				themeItem.setSelected(true);
			}
			themeItems[i] = themeItem;
			themeMenu.add(themeItem);
			i++;
		}
		add(themeMenu);
		JMenuItem githubItem = new JMenuItem();
		JMenuItem wiki = new JMenuItem();
		translatableComponents.put(githubItem, "mcp.github");
		translatableComponents.put(wiki, "mcp.wiki");
		githubItem.addActionListener(e -> Util.openUrl(MCP.GITHUB_URL));
		wiki.addActionListener(e -> Util.openUrl(MCP.GITHUB_URL + "/wiki"));
		helpMenu.add(githubItem);
		helpMenu.add(wiki);
		add(helpMenu);
	}

	public void reloadSide() {
		for (JMenuItem sideItem : sideItems) {
			if (sideItem != null) {
				sideItem.setSelected(false);
			}
		}
		int itemNumber = mcp.getSide().index;
		if (itemNumber < 0) {
			itemNumber = sideItems.length - 1;
		}
		sideItems[itemNumber].setSelected(true);
	}

	public void reloadOptions() {
		for (Map.Entry<TaskParameter, JMenuItem> entry : optionItems.entrySet()) {
			TaskParameter param = entry.getKey();
			JMenuItem item = entry.getValue();
			if (param.type == String[].class) {
				// "Active" for an array-valued option = at least one entry. Lets
				// us render it as a toggle in the menu while still keeping the
				// underlying value a comma-separated list.
				String[] arr = mcp.options.getStringArrayParameter(param);
				item.setSelected(arr != null && arr.length > 0);
			} else if (param.type == Boolean.class) {
				item.setSelected(mcp.options.getBooleanParameter(param));
			}
		}
	}

	private void initOptions() {
		JMenu sideMenu = new JMenu();
		translatableComponents.put(sideMenu, "mcp.side");
		sideItems = new JMenuItem[Side.VALUES.length];
		for (Side side : Side.VALUES) {
			final int i = side.index;
			if (i >= 0) {
				sideItems[i] = new JRadioButtonMenuItem(side.getName());
				sideItems[i].addActionListener(e -> mcp.setSide(side));
				sideMenu.add(sideItems[i]);
			}
		}
		sideItems[sideItems.length - 1] = new JRadioButtonMenuItem(Side.ANY.getName());
		sideItems[sideItems.length - 1].addActionListener(e -> mcp.setSide(Side.ANY));
		sideMenu.add(sideItems[sideItems.length - 1]);
		menuOptions.add(sideMenu);

		String[] names = MainGUI.TABS;
		TaskParameter[][] params = MainGUI.TAB_PARAMETERS;
		for (int i = 0; i < names.length; i++) {
			JMenu a = new JMenu();
			translatableComponents.put(a, names[i]);
			for (TaskParameter param : params[i]) {
				JMenuItem b;
				if (param.type == Boolean.class) {
					b = new JRadioButtonMenuItem();
					translatableComponents.put(b, "task.param." + param.name);
					optionItems.put(param, b);
					b.addActionListener(e -> {
						mcp.options.setParameter(param, b.isSelected());
						mcp.options.save();
					});
				} else if (param.type == String[].class) {
					// Array-valued option rendered as a checkbox: enabling it
					// pops up the value editor; an empty/cancelled value reverts
					// to disabled. Disabling clears the array. Lets the user
					// toggle expensive opt-in features (like string-literal
					// remapping for ASM/transformer code) without leaving the
					// menu.
					b = new JRadioButtonMenuItem();
					translatableComponents.put(b, "task.param." + param.name);
					optionItems.put(param, b);
					final JRadioButtonMenuItem checkbox = (JRadioButtonMenuItem) b;
					b.addActionListener(e -> {
						if (checkbox.isSelected()) {
							mcp.inputOptionsValue(param);
							String[] result = mcp.options.getStringArrayParameter(param);
							if (result == null || result.length == 0) {
								// User cancelled or supplied an empty value —
								// revert the checkbox so its visible state stays
								// in sync with the underlying option.
								checkbox.setSelected(false);
							}
						} else {
							mcp.options.setParameter(param, new String[0]);
							mcp.options.save();
						}
					});
				} else {
					b = new JMenuItem(param.getDesc());
					b.addActionListener(u -> mcp.inputOptionsValue(param));
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

	protected void setComponentsEnabled(boolean b) {
		start.forEach((key, value) -> value.setEnabled(TaskMode.START.isAvailable(mcp, key)));
		taskItems.forEach((key, value) -> value.setEnabled(key.isAvailable(mcp, mcp.getSide())));
		for (JMenuItem item : togglableComponents) {
			item.setEnabled(b);
		}
	}

	/**
	 * Reloads text on all translatable components
	 */
	public void reloadText() {
		for (Entry<JMenuItem, String> entry : translatableComponents.entrySet()) {
			entry.getKey().setText(MCP.TRANSLATOR.translateKey(entry.getValue()));
		}
		for (Entry<TaskMode, JMenuItem> entry : taskItems.entrySet()) {
			entry.getValue().setText(entry.getKey().getFullName());
			entry.getValue().setToolTipText(entry.getKey().getDesc());
		}
	}
}
