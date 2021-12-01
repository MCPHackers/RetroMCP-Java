package org.mcphackers.mcp.tools;

import de.fernflower.main.extern.IFernflowerLogger;

public class DecompileLog extends IFernflowerLogger {

	public DecompileLog() {
		numberOfClasses = 0;
		currentClassNumber = 0;
		currentMessage = "";
	}
	
	private int numberOfClasses;
	private int currentClassNumber;
	private String currentMessage;
	
	public DecompileInfo initInfo()
	{
		return new DecompileInfo(currentMessage, currentClassNumber, numberOfClasses);
	}

	@Override
	public void writeMessage(String message, Severity severity) {
		if (accepts(severity)) {
			currentMessage = severity.prefix + message;
		}
	}

	@Override
	public void writeMessage(String message, Throwable t) {
		if (accepts(Severity.ERROR)) {
			currentMessage = Severity.ERROR.prefix + message;
		}
	}

	public void startReadingClass(String className) {
		if (accepts(Severity.INFO)) {
			currentMessage = "Decompiling class " + className;
		}
	}

	public void updateCounters(int i, int i2) {
		currentClassNumber = i;
		numberOfClasses = i2;
	}

}
