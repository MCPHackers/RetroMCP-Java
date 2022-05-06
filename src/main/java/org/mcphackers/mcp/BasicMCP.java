package org.mcphackers.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.mode.TaskMode;

public abstract class BasicMCP extends MCP {

	@Override
	public Path getWorkingDir() {
		return Paths.get("");
	}

	@Override
	public void setProgressBars(List<Task> tasks, TaskMode mode) {
	}

	@Override
	public void clearProgressBars() {
	}

	@Override
	public void setProgress(int barIndex, String progressMessage) {
	}

	@Override
	public void setProgress(int barIndex, int progress) {
	}

	@Override
	public void setActive(boolean active) {
	}

}
