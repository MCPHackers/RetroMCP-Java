package org.mcphackers.mcp.tasks.mode;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.tasks.Task.Side;

/**
 * Any optional parameters which can be accessed from tasks
 * @see Options#getParameter(TaskParameter)
 *
 */
public enum TaskParameter {

	SIDE("side", Integer.class, Side.ANY.index),
	PATCHES("patch", Boolean.class, true),
	IGNORED_PACKAGES("ignore", String[].class, new String[] {"paulscode", "com/jcraft", "de/jarnbjo", "isom"}),
	INDENTATION_STRING("ind", String.class, "\t"),
	OBFUSCATION("obf", Boolean.class, false),
	FULL_BUILD("fullbuild", Boolean.class, false),
	RUN_BUILD("runbuild", Boolean.class, false),
	RUN_ARGS("runargs", String[].class, new String[] {"-Xms1024M", "-Xmx1024M"}),
	GAME_ARGS("gameargs", String.class, ""),
	SETUP_VERSION("setup", String.class, null),
	SOURCE_VERSION("source", Integer.class, -1),
	TARGET_VERSION("target", Integer.class, -1),
	JAVA_HOME("javahome", String.class, ""),
	DECOMPILE_OVERRIDE("override", Boolean.class, false),
	DECOMPILE_RESOURCES("resources", Boolean.class, false),
	GUESS_GENERICS("generics", Boolean.class, false),
	STRIP_GENERICS("stripgenerics", Boolean.class, false),
	OUTPUT_SRC("outputsrc", Boolean.class, true);

	public static final TaskParameter[] VALUES = TaskParameter.values();

	public final String name;
	public final Class<?> type;
	public final Object defaultValue;

	TaskParameter(String name, Class<?> c, Object value) {
		TaskMode.nameToParamMap.put(name, this);
		this.name = name;
		this.type = c;
		this.defaultValue = value;
	}

	public String getDesc() {
		String s = "task.param." + name;
		if(MCP.TRANSLATOR.hasKey(s)) {
			return MCP.TRANSLATOR.translateKey(s);
		}
		return MCP.TRANSLATOR.translateKey("task.noDesc");
	}
}
