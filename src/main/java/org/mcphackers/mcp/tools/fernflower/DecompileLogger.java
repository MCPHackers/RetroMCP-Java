package org.mcphackers.mcp.tools.fernflower;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.ProgressListener;

import de.fernflower.main.extern.IFernflowerLogger;

public class DecompileLogger extends IFernflowerLogger {

	private final ProgressListener listener;

	public DecompileLogger(ProgressListener listener) {
		this.listener = listener;
	}

	public void writeMessage(String message, Severity severity) {
		if(severity.ordinal() >= Severity.WARN.ordinal()) {
//			System.out.println(message);
		}
	}

	public void writeMessage(String message, Throwable t) {
//		System.out.println(message);
//		t.printStackTrace();
	}

	public void startReadingClass(String className) {
		listener.setProgress(MCP.TRANSLATOR.translateKey("task.stage.decompile") + " " + className);
	}

	@Override
	public void updateCounters(int current, int total) {
		listener.setProgress((int)((double)current/(double)total * 100));
	}

}
