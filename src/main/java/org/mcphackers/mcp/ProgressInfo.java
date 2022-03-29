package org.mcphackers.mcp;

public class ProgressInfo {

	private final String msg;
	private final int[] progress = new int[2];

	public ProgressInfo(String currentMessage, int current, int total) {
		msg = currentMessage;
		progress[0] = current;
		progress[1] = total;
	}

	public String getMessage() {
		return this.msg;
	}

	public int getTotal() {
		return this.progress[1];
	}

	public int getCurrent() {
		return this.progress[0];
	}

}
