package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tools.Decompiler;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;

public class TaskDecompile extends Task {

	private Decompiler decompiler;
	private int step;
	
	public TaskDecompile(int side) {
		super(side);
		step = 0;
		decompiler = new Decompiler(side);
	}

	public void doTask() throws Exception {
		step = 0;
		Utility.runCommand("java -jar retroguard.jar");
		step = 1;
		decompiler.start();
	}

	public ProgressInfo getProgress() {
		switch(step) {
		case 0:
			return new ProgressInfo("Renaming...", 0, 1);
		default:
			return decompiler.log.initInfo();
		}
	}

}
