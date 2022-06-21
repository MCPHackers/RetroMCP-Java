package org.mcphackers.mcp.tasks.mode;

import org.mcphackers.mcp.MCP;

public enum TaskParameter {
	
	DEBUG("debug", "Display additional info", Boolean.class),
	SIDE("side", "Set side", Integer.class),
	SRC_CLEANUP("src", "Source cleanup", Boolean.class),
	PATCHES("patch", "Apply patches", Boolean.class),
	IGNORED_PACKAGES("ignore", "Set ignored packages", String[].class),
	INDENTATION_STRING("ind", "Set indentation string", String.class),
	OBFUSCATION("obf", "Obfuscate mod", Boolean.class),
	FULL_BUILD("fullbuild", "Full build", Boolean.class),
	RUN_BUILD("runbuild", "Run build", Boolean.class),
	RUN_ARGS("runargs", "Run arguments", String[].class),
	SETUP_VERSION("setup", "Setup version", String.class),
	SOURCE_VERSION("source", "Set a specific source version", Integer.class),
	TARGET_VERSION("target", "Set a specific target version", Integer.class),
	JAVA_HOME("javahome", "Set JAVA_HOME used for compiling", String.class),
	DECOMPILE_OVERRIDE("override", "Decompile @Override", Boolean.class);

	public final String desc;
	public final String name;
	public final Class<?> type;
	public final String translatedDesc;

	TaskParameter(String name, Class<?> c) {
		this(name, "No description provided", c);
	}

	TaskParameter(String name, String desc, Class<?> c) {
		TaskMode.nameToParamMap.put(name, this);
		this.name = name;
		this.desc = desc;
		this.translatedDesc = MCP.TRANSLATOR.translateKey(desc.toLowerCase().replaceAll(" ", "_"));
		this.type = c;
	}
}
