package org.mcphackers.mcp.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {
	public PluginClassLoader(ClassLoader parent) {
		super(new URL[] {}, parent);
	}

	public void addURL(URL url) {
		super.addURL(url);
	}
}
