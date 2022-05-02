package org.mcphackers.mcp.gui;

import javax.swing.JProgressBar;

public class SideProgressBar extends JProgressBar {
	
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
