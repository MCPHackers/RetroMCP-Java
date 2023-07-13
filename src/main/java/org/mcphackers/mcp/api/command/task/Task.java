package org.mcphackers.mcp.api.command.task;

import org.mcphackers.mcp.api.command.Command;

public abstract class Task extends Command implements Runnable {
	public Task(String name, String longDescriptionKey, String shortDescriptionKey) {
		super(name, longDescriptionKey, shortDescriptionKey, () -> {});
		this.setRunnable(this);
	}

	@Override
	public void run() {

	}
}
