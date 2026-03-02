package org.mcphackers.mcp.tools;

import java.util.Locale;

public enum OS {

	linux,
	windows,
	osx,
	unknown;

	public static OS os;


	public static OS getOs() {
		if (os == null) {
			String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			if (osName.contains("mac") || osName.contains("darwin")) {
				os = osx;
			} else if (osName.contains("win")) {
				os = windows;
			} else if (osName.contains("nix") || osName.contains("nux")
					|| osName.contains("aix") || osName.contains("sunos")) {
				os = linux;
			} else {
				os = unknown;
			}
		}
		return os;
	}

	public static boolean isARM32() {
		return System.getProperty("os.arch").equals("arm");
	}

	public static boolean isARM64() {
		return System.getProperty("os.arch").equals("aarch64");
	}

	public static boolean isMSeries() {
		return os == osx && isARM64();
	}
}
