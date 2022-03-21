package org.mcphackers.mcp.gui;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.mcphackers.mcp.tools.Util;

public class MenuBar extends JMenuBar {
	private final JMenu menuOptions = new JMenu("Options");
	private final JMenu helpMenu = new JMenu("Help");
	private final JMenuItem githubItem = new JMenuItem("Github Page");

	public MenuBar() {
		this.menuOptions.setMnemonic(KeyEvent.VK_O);
		this.helpMenu.setMnemonic(KeyEvent.VK_H);
		add(menuOptions);
		this.githubItem.addActionListener(_e -> this.onGithubClicked());
		this.helpMenu.add(this.githubItem);
		add(helpMenu);
	}

	private void onGithubClicked() {
		Util.openUrl("https://github.com/MCPHackers/RetroMCP-Java");
	}

	public JMenu getOptions() {
		return menuOptions;
	}
}
