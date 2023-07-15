package org.mcphackers.mcp.api.task;

public abstract class Task implements RunnableTask {
	private final String name;
	private final String longDescriptionKey;
	private final String shortDescriptionKey;

	public Task(String name, String longDescriptionKey, String shortDescriptionKey) {
		this.name = name;
		this.longDescriptionKey = longDescriptionKey;
		this.shortDescriptionKey = shortDescriptionKey;
	}

	public String getName() {
		return this.name;
	}

	public String getLongDescriptionKey() {
		return this.longDescriptionKey;
	}

	public String getShortDescriptionKey() {
		return this.shortDescriptionKey;
	}
}
