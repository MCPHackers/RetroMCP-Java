package org.mcphackers.mcp.tools;

import de.fernflower.main.extern.IFernflowerLogger;

public class DecompileLogger extends IFernflowerLogger {

	public DecompileLogger() {
		numberOfClasses = 0;
		currentClassNumber = 0;
		currentMessage = "";
	}
	
	private int numberOfClasses;
	private int currentClassNumber;
	private String currentMessage;
	
	public ProgressInfo initInfo()
	{
		return new ProgressInfo(currentMessage, currentClassNumber, numberOfClasses);
	}

	public void writeMessage(String message, Severity severity) {
	}

	public void writeMessage(String message, Throwable t) {
	}

	public void startReadingClass(String className) {
		currentMessage = "Decompiling class " + className;
	}

	public void updateCounters(int i, int i2) {
		currentClassNumber = i;
		numberOfClasses = i2;
	}

}
