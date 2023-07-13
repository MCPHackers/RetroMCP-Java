package org.mcphackers.mcp.api.command;

import org.mcphackers.mcp.api.language.LanguageManager;

public class Command {
	private final String name;
	private final String longDescriptionKey;
	private final String shortDescriptionKey;
	private Runnable runnable;

	public Command(String name, String longDescriptionKey, String shortDescriptionKey, Runnable runnable) {
		this.name = name;
		this.longDescriptionKey = longDescriptionKey;
		this.shortDescriptionKey = shortDescriptionKey;
		this.runnable = runnable;
	}

	public String getName() {
		return this.name;
	}

	public String getLongDescription(LanguageManager languageManager) {
		return languageManager.translate(this.longDescriptionKey);
	}

	public String getShortDescription(LanguageManager languageManager) {
		return languageManager.translate(this.shortDescriptionKey);
	}

	public Runnable getRunnable() {
		return this.runnable;
	}

	public void setRunnable(Runnable runnable) {
		this.runnable = runnable;
	}
}
