package org.mcphackers.mcp.tools;

public class ProgressInfo {

	public String msg;
	public int[] progress = new int[2];
	
	public ProgressInfo(String currentMessage, int current, int total) {
		msg = currentMessage;
		progress[0] = current;
		progress[1] = total;
	}

}
