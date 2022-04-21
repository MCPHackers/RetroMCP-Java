package org.mcphackers.mcp;

public enum TaskParameter {
	
	DEBUG("debug", "Display additional info", Boolean.class),
	SIDE("side", "Set side", Integer.class),
	SRC_CLEANUP("src", "Source cleanup", Boolean.class),
	PATCHES("patch", "Apply patches", Boolean.class),
	IGNORED_PACKAGES("ignore", "Set ignored packages", String[].class),
	INDENTION_STRING("ind", "Set indention string", String.class),
	OBFUSCATION("obf", "Obfuscation", Boolean.class),
	FULL_BUILD("fullbuild", "Full build", Boolean.class),
	RUN_BUILD("runbuild", "Run build", Boolean.class),
	RUN_ARGS("runargs", "Run arguments", String[].class),
	SETUP_VERSION("setup", "Setup version", String.class),
	SOURCE_VERSION("source", "Set a specific source version", Integer.class),
	TARGET_VERSION("target", "Set a specific target version", Integer.class),
	BOOT_CLASS_PATH("bootclasspath", "Set a specific bootstrap class path", String.class);
	
	public final String desc;
	public final String name;
	public final Class<?> type;
	
	TaskParameter(String name, Class<?> c) {
		this(name, "No description provided", c);
	}
	
	TaskParameter(String name, String desc, Class<?> c) {
		MCP.nameToParamMap.put(name, this);
		this.name = name;
		this.desc = desc;
		this.type = c;
	}
}
