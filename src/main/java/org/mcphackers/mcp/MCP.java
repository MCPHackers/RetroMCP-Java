package org.mcphackers.mcp;

import org.mcphackers.mcp.api.language.LanguageManager;

public abstract class MCP {
	private final LanguageManager languageManager = new LanguageManager();

	public LanguageManager getLanguageManager() {
		return this.languageManager;
	}
}
