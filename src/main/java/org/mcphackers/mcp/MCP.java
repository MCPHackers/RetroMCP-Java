package org.mcphackers.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mcphackers.mcp.api.language.LanguageManager;
import org.mcphackers.mcp.api.logging.LoggingManager;
import org.mcphackers.mcp.api.plugin.MCPPlugin;
import org.mcphackers.mcp.api.plugin.PluginManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class MCP {
	public static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
	private final LanguageManager languageManager = new LanguageManager();
	private final LoggingManager loggingManager = new LoggingManager();
	private final PluginManager pluginManager = new PluginManager();
	private Path workingDirectory = Paths.get(".");

	public void initializeMCP() {
		this.pluginManager.discoverPlugins(this);
	}

	public LanguageManager getLanguageManager() {
		return this.languageManager;
	}

	public LoggingManager getLoggingManager() {
		return this.loggingManager;
	}

	public PluginManager getPluginManager() {
		return this.pluginManager;
	}

	public List<MCPPlugin> getLoadedPlugins() {
		return this.pluginManager.getLoadedPlugins();
	}

	public Path getWorkingDirectory() {
		return this.workingDirectory;
	}

	public void setWorkingDirectory(Path workingDirectory) {
		this.workingDirectory = workingDirectory;
	}
}
