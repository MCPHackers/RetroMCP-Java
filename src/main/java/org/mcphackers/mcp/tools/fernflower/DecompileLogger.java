package org.mcphackers.mcp.tools.fernflower;

import org.mcphackers.mcp.tasks.ProgressListener;

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;

public class DecompileLogger extends IFernflowerLogger {

	private final ProgressListener listener;
	private int total;

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
		listener.setProgress("Decompiling class " + className);
	}

	@Override
	public void startSave(int total) {
		this.total = total;
	}

	@Override
	public void updateSave(int current) {
		listener.setProgress((int)((double)current/(double)total * 100));
	}

}
