package org.mcphackers.mcp.tools;

import java.util.Locale;

public enum Os {
	LINUX,
	OSX,
	WINDOWS,
	OTHER;

	private static Os os = null;

	public static Os getOs() {
		if (os == null) {
			String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			if (osName.contains("mac") || osName.contains("darwin")) {
				os = OSX;
			} else if (osName.contains("win")) {
				os = WINDOWS;
			} else if (osName.contains("nix") || osName.contains("nux")
					|| osName.contains("aix") || osName.contains("sunos")) {
				os = LINUX;
			} else {
				os = OTHER;
			}
		}
		return os;
	}

}