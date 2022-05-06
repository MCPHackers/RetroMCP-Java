package org.mcphackers.mcp.tools.fernflower;

import org.mcphackers.mcp.tasks.ProgressListener;

import de.fernflower.main.extern.IFernflowerLogger;

public class DecompileLogger extends IFernflowerLogger {

	private final ProgressListener listener;

	public DecompileLogger(ProgressListener listener) {
		this.listener = listener;
	}

	public void writeMessage(String message, Severity severity) {
	}

	public void writeMessage(String message, Throwable t) {
	}

	public void startReadingClass(String className) {
		listener.setProgress("Decompiling class " + className);
	}

	public void updateCounters(int i, int i2) {
		listener.setProgress((int)((double)i/(double)i2 * 100));
	}

}
