package org.mcphackers.mcp.tools;

public class DecompileInfo {

	public String msg;
	public int[] counters = new int[2];
	
	public DecompileInfo(String currentMessage, int currentClassNumber, int numberOfClasses) {
		msg = currentMessage;
		counters[0] = currentClassNumber;
		counters[1] = numberOfClasses;
	}

}
