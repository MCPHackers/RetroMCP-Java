package org.mcphackers.mcp;

public interface ProgressListener {
	
	void setProgress(String progressMessage);
	
	void setProgress(int progress);
	
	default void setProgress(String progressMessage, int progress) {
		setProgress(progressMessage);
		setProgress(progress);
	}
	
}
