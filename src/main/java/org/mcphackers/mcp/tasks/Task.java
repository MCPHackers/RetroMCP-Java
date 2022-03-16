package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.ProgressListener;

public abstract class Task implements ProgressListener {
	
	protected static final int CLIENT = 0;
	protected static final int SERVER = 1;
	
	protected final int side;
	protected final MCP mcp;
	protected int step;
	private ProgressListener progressListener;
	
	protected Task(int side, MCP mcp, ProgressListener listener) {
		this(side, mcp);
		this.progressListener = listener;
	}
	
	protected Task(int side, MCP mcp) {
		this.side = side;
		this.mcp = mcp;
	}

	public abstract void doTask() throws Exception;
	
	protected void step() {
		step++;
		updateProgress();
	}
	
	protected void updateProgress() {
		setProgress("Idle", 0);
	}

	public void setProgress(String progressString) {
		if(progressListener == null) {
			mcp.setProgress(side, progressString);
		}
		else {
			progressListener.setProgress(progressString);
		}
	}

	public void setProgress(int progress) {
		if(progressListener == null) {
			mcp.setProgress(side, progress);
		}
		else {
			progressListener.setProgress(progress);
		}
	}
	
	protected void log(String msg) {
		mcp.log(msg);
	}
	
	protected String chooseFromSide(String... strings) {
		if(side < strings.length) {
			return strings[side];
		}
		return null;
	}
}
