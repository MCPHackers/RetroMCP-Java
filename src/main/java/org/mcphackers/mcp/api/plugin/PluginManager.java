package org.mcphackers.mcp.api.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.api.util.FileUtilities;

public class PluginManager {
	private final List<MCPPlugin> loadedPlugins = new ArrayList<>();

	public List<MCPPlugin> getLoadedPlugins() {
		return this.loadedPlugins;
	}

	public void discoverPlugins(MCP mcp) {
		ClassLoader classLoader = mcp.getClass().getClassLoader();
		try (PluginClassLoader pluginClassLoader = new PluginClassLoader(classLoader)) {
			Path pluginsDir = mcp.getWorkingDirectory().resolve("plugins");
			if (Files.exists(pluginsDir) && Files.isDirectory(pluginsDir)) {
				List<Path> pluginCandidates = FileUtilities.getPathsOfType(pluginsDir, ".jar", ".zip");
				for (Path pluginCandidate : pluginCandidates) {
					System.out.println("Adding " + pluginCandidate.getFileName() + " to classpath!");
					pluginClassLoader.addURL(pluginCandidate.toUri().toURL());
				}

				ServiceLoader<MCPPlugin> serviceLoader = ServiceLoader.load(MCPPlugin.class, pluginClassLoader);
				for (MCPPlugin plugin : serviceLoader) {
					System.out.println("Found service!");
					plugin.initializePlugin(mcp);
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
