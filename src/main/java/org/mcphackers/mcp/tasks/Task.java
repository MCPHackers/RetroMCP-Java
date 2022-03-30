package org.mcphackers.mcp.tasks;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.ProgressListener;
import org.mcphackers.mcp.MCP;

public abstract class Task implements ProgressListener {
	
	public enum Side {
		NONE(-1, "None"),
		CLIENT(0, "Client"),
		SERVER(1, "Server");
		
		public final int index;
		public final String name;
		
		Side(int index, String name) {
			this.index = index;
			this.name = name;
		}
	}
	
	public static final byte INFO = 0;
	public static final byte WARNING = 1;
	public static final byte ERROR = 2;
	
	public final Side side;
	private int progressBarIndex = -1; 
	protected final MCP mcp;
	protected int step;
	private byte result = INFO;
	private ProgressListener progressListener;
	private final List<String> logMessages = new ArrayList<>();
	
	protected Task(Side side, MCP instance, ProgressListener listener) {
		this(side, instance);
		this.progressListener = listener;
	}
	
	protected Task(Side side, MCP instance) {
		this.side = side;
		this.mcp = instance;
	}

	public abstract void doTask() throws Exception;
	
	protected void step() {
		step++;
		updateProgress();
	}
	
	public abstract String getName();
	
	protected void addMessage(String msg, byte logLevel) {
		if(progressListener != null) {
			if(progressListener instanceof Task) {
				Task task = (Task)progressListener;
				task.addMessage(msg, logLevel);
			}
		}
		logMessages.add(msg);
		result = logLevel < result ? result : logLevel;
	}

	public byte getResult() {
		return result;
	}
	
	public List<String> getMessageList() {
		return logMessages;
	}
	
	protected void updateProgress() {
		setProgress("Idle", 0);
	}

	public void setProgress(String progressString) {
		if(progressListener == null) {
			mcp.setProgress(progressBarIndex, progressString);
		}
		else {
			progressListener.setProgress(progressString);
		}
	}

	public void setProgress(int progress) {
		if(progressListener == null) {
			mcp.setProgress(progressBarIndex, progress);
		}
		else {
			progressListener.setProgress(progress);
		}
	}
	
	protected void log(String msg) {
		mcp.log(msg);
	}
	
	public void setProgressBarIndex(int i) {
		progressBarIndex = i;
	}
	
	protected String chooseFromSide(String... strings) {
		if(side.index >= 0 && side.index < strings.length) {
			return strings[side.index];
		}
		return null;
	}
}
