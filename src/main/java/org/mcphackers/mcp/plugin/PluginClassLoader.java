package org.mcphackers.mcp.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {
	/**
	 * @param parent Parent class loader for the plugin class loader
	 */
	public PluginClassLoader(ClassLoader parent) {
		super(new URL[] {}, parent);
	}

	/**
	 * Appends the specified URL to the list of URLs to search for
	 * classes and resources.
	 * <p>
	 * If the URL specified is {@code null} or is already in the
	 * list of URLs, or if this loader is closed, then invoking this
	 * method has no effect.
	 *
	 * @param url the URL to be added to the search path of URLs
	 */
	public void addURL(URL url) {
		super.addURL(url);
	}
}
