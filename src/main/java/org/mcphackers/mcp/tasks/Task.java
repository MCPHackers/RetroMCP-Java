package org.mcphackers.mcp.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mcphackers.mcp.plugin.MCPPlugin.TaskEvent;
import org.mcphackers.mcp.MCP;

public abstract class Task implements ProgressListener, TaskRunnable {
	
	public static enum Side {
		ANY(-1, "any"),
		CLIENT(0, "client"),
		SERVER(1, "server"),
		MERGED(2, "merged");
		
		public static final Side[] ALL = {CLIENT, SERVER, MERGED};
		
		public final int index;
		public final String name;
		
		Side(int index, String name) {
			this.index = index;
			this.name = name;
			sides.put(index, this);
		}
		
		public String getName() {
			return MCP.TRANSLATOR.translateKey("side." + name);
		}
	}
	
	public static final Map<Integer, Side> sides = new HashMap<>();
	
	public static final byte INFO = 0;
	public static final byte WARNING = 1;
	public static final byte ERROR = 2;
	
	public final Side side;
	protected final MCP mcp;
	private byte result = INFO;
	private ProgressListener progressListener;
	private int progressBarIndex = -1; 
	private final List<String> logMessages = new ArrayList<>();
	
	public Task(Side side, MCP instance, ProgressListener listener) {
		this(side, instance);
		this.progressListener = listener;
	}
	
	public Task(Side side, MCP instance) {
		this.side = side;
		this.mcp = instance;
	}
	
	public Task(MCP instance) {
		this(Side.ANY, instance);
	}

	public final void performTask() throws Exception {
		triggerEvent(TaskEvent.PRE_TASK);
		doTask();
		triggerEvent(TaskEvent.POST_TASK);
	}
	
	protected final void triggerEvent(TaskEvent event) {
		mcp.triggerTaskEvent(event, this);
	}
	
	protected final void addMessage(String msg, byte logLevel) {
		if(progressListener != null) {
			if(progressListener instanceof Task) {
				Task task = (Task)progressListener;
				task.addMessage(msg, logLevel);
			}
		}
		logMessages.add(msg);
		result = logLevel < result ? result : logLevel;
	}

	public final byte getResult() {
		return result;
	}
	
	public final List<String> getMessageList() {
		return logMessages;
	}

	public void setProgress(String progressString) {
		if(progressListener != null) {
			progressListener.setProgress(progressString);
		}
		else if(progressBarIndex >= 0) {
			mcp.setProgress(progressBarIndex, progressString);
		}
	}

	public void setProgress(int progress) {
		if(progressListener != null) {
			progressListener.setProgress(progress);
		}
		else if(progressBarIndex >= 0) {
			mcp.setProgress(progressBarIndex, progress);
		}
	}
	
	public void log(String msg) {
		mcp.log(msg);
	}
	
	public void setProgressBarIndex(int i) {
		progressBarIndex = i;
	}
	
	public final String getLocalizedStage(String stage, Object... formatting) {
		return MCP.TRANSLATOR.translateKeyWithFormatting("task.stage." + stage, formatting);
	}
}
