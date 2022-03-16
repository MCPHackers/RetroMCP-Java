package org.mcphackers.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public interface MCP {
	
	String VERSION = "v0.3";

	default void attemptToDeleteUpdateJar() {
		long startTime = System.currentTimeMillis();
		boolean keepTrying = true;
		while(keepTrying) {
			try {
				Files.deleteIfExists(Paths.get(MCPPaths.UPDATE_JAR));
				keepTrying = false;
			} catch (IOException e) {
				keepTrying = System.currentTimeMillis() - startTime < 10000;
			}
		}
	}

	void log(String msg);

	boolean getBooleanParam(TaskParameter param);

	String[] getStringArrayParam(TaskParameter param);

	String getStringParam(TaskParameter param);
	
	void setProgressBarActive(int side, boolean active);
	
	void setProgress(int side, String progressMessage);
	
	void setProgress(int side, int progress);
	
	default void setProgress(int side, String progressMessage, int progress) {
		setProgress(side, progress);
		setProgress(side, progressMessage);
	}
}
