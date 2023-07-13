package org.mcphackers.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mcphackers.mcp.api.language.LanguageManager;
import org.mcphackers.mcp.api.logging.LoggingManager;

public abstract class MCP {
	public static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
	private final LanguageManager languageManager = new LanguageManager();
	private final LoggingManager loggingManager = new LoggingManager();

	public LanguageManager getLanguageManager() {
		return this.languageManager;
	}

	public LoggingManager getLoggingManager() {
		return this.loggingManager;
	}
}
