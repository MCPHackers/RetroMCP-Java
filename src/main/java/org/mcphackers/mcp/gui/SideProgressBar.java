package org.mcphackers.mcp.gui;

import javax.swing.JProgressBar;

public class SideProgressBar extends JProgressBar {

	private static final long serialVersionUID = -8002821179520037516L;

	public String progressMsg;
	public int progress;

	public SideProgressBar() {
		setStringPainted(true);
	}

	public void updateProgress() {
		setValue(progress);
		setString(progress + "% " + progressMsg);
	}

}
