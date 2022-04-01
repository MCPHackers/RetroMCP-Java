package org.mcphackers.mcp;

public enum TaskParameter {
	
	DEBUG("debug", "Display additional info"),
	SIDE("side", "Perform operation for side"),
	SRC("src", "Source cleanup"),
	PATCHES("patch", "Apply patches"),
	IGNORED_PACKAGES("ignore", "Ignored packages"),
	INDENTION_STRING("ind", "Indention string"),
	FULL_BUILD("fullbuild", "Full build"),
	RUN_BUILD("runbuild", "Run build"),
	RUN_ARGS("runargs", "Run arguments"),
	SETUP_VERSION("setup", "Setup version"),
	SOURCE_VERSION("source", "Set a specific source version"),
	TARGET_VERSION("target", "Set a specific target version"),
	BOOT_CLASS_PATH("bootclasspath", "Set a specific bootstrap class path");
	
	public final String desc;
	public final String name;
	
	TaskParameter(String name) {
		this(name, "No description provided");
	}
	
	TaskParameter(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}
}
