package org.mcphackers.mcp.tools;

public class ProgressInfo {

    private String msg;
    private int[] progress = new int[2];

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
