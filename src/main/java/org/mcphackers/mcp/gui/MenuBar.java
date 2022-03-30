package org.mcphackers.mcp.gui;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskDownloadUpdate;
import org.mcphackers.mcp.tools.Util;

public class MenuBar extends JMenuBar {
	public final JMenu menuOptions = new JMenu("Options");
	public final JMenu mcpMenu = new JMenu("MCP");
	private final JMenu helpMenu = new JMenu("Help");
	private final JMenuItem[] sideItems = new JMenuItem[3];
	private final JMenuItem githubItem = new JMenuItem("Github Page");
	private final MainGUI owner;

	public MenuBar(MainGUI mainGUI) {
		owner = mainGUI;
		this.menuOptions.setMnemonic(KeyEvent.VK_O);
		this.helpMenu.setMnemonic(KeyEvent.VK_H);
		initOptions();
		reloadOptions(owner);
		JMenuItem update = new JMenuItem("Check for updates");
		update.addActionListener(a -> {
			owner.operateOnThread(() -> {
				owner.setAllButtonsActive(false);
    			try {
					new TaskDownloadUpdate(owner).doTask();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
    			owner.setAllButtonsActive(true);
			});
		});
		mcpMenu.add(update);
		add(mcpMenu);
		add(menuOptions);
		this.githubItem.addActionListener(e -> this.onGithubClicked());
		this.helpMenu.add(this.githubItem);
		add(helpMenu);
	}

	private void reloadOptions(MainGUI mainGUI) {
		for(int i = 0; i < sideItems.length; i++) {
			sideItems[i].setSelected(false);
		}
		sideItems[mainGUI.side].setSelected(true);
	}
	
	private void setSide(int i) {
		owner.side = i;
		reloadOptions(owner);
	}

	private void initOptions() {
		JMenu sideMenu = new JMenu("Side");
		String[] sideNames = new String[] {Side.CLIENT.name, Side.SERVER.name, "Both"};
		for(int i = 0; i < sideItems.length; i++) {
			final int i2 = i;
			sideItems[i] = new JRadioButtonMenuItem(sideNames[i]);
			sideItems[i].addActionListener(e -> setSide(i2));
			sideMenu.add(sideItems[i]);
		}
		menuOptions.add(sideMenu);
		
		//FIXME
		JMenuItem a = new JMenuItem("More options...");
		a.setEnabled(false);
		menuOptions.add(a);
	}

	private void onGithubClicked() {
		Util.openUrl("https://github.com/MCPHackers/RetroMCP-Java");
	}
}
