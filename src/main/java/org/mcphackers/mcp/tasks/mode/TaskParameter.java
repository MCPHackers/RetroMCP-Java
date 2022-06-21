package org.mcphackers.mcp.tasks.mode;

import org.mcphackers.mcp.MCP;

public enum TaskParameter {
	
	DEBUG("debug", Boolean.class),
	SIDE("side", Integer.class),
	SRC_CLEANUP("src", Boolean.class),
	PATCHES("patch", Boolean.class),
	IGNORED_PACKAGES("ignore", String[].class),
	INDENTATION_STRING("ind", String.class),
	OBFUSCATION("obf", Boolean.class),
	FULL_BUILD("fullbuild", Boolean.class),
	RUN_BUILD("runbuild", Boolean.class),
	RUN_ARGS("runargs", String[].class),
	SETUP_VERSION("setup", String.class),
	SOURCE_VERSION("source", Integer.class),
	TARGET_VERSION("target", Integer.class),
	JAVA_HOME("javahome", String.class),
	DECOMPILE_OVERRIDE("override", Boolean.class);

	public final String name;
	public final Class<?> type;

	TaskParameter(String name, Class<?> c) {
		TaskMode.nameToParamMap.put(name, this);
		this.name = name;
		this.type = c;
	}
	
	public String getDesc() {
		String s = "task.param." + name;
		if(MCP.TRANSLATOR.hasKey(s)) {
			return MCP.TRANSLATOR.translateKey(s);
		}
		return MCP.TRANSLATOR.translateKey("task.noDesc");
	}
}
