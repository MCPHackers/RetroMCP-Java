package org.mcphackers.mcp.tasks.mode;

import de.fernflower.main.extern.IFernflowerPreferences;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.tasks.Task.Side;

import java.util.HashMap;
import java.util.Map;


/**
 * Any optional parameters which can be accessed from tasks
 *
 * @see Options#getParameter(TaskParameter)
 */
public enum TaskParameter {

	SIDE("side", Integer.class, Side.ANY.index),
	PATCHES("patch", Boolean.class, true),
	IGNORED_PACKAGES("ignore", String[].class, new String[]{"paulscode", "com/jcraft", "de/jarnbjo", "isom"}),
	FERNFLOWER_OPTIONS("ff_options", String.class, getDefaultFFOptions()),
	OBFUSCATION("obf", Boolean.class, false),
	FULL_BUILD("fullbuild", Boolean.class, false),
	RUN_BUILD("runbuild", Boolean.class, false),
	RUN_ARGS("runargs", String[].class, new String[]{"-Xms1024M", "-Xmx1024M"}),
	GAME_ARGS("gameargs", String.class, ""),
	SETUP_VERSION("setup", String.class, null),
	SOURCE_VERSION("source", Integer.class, -1),
	TARGET_VERSION("target", Integer.class, -1),
	JAVA_HOME("javahome", String.class, ""),
	EXCLUDED_CLASSES("excludedclasses", String.class, ""),
	DECOMPILE_RESOURCES("resources", Boolean.class, false),
	GUESS_GENERICS("generics", Boolean.class, false),
	STRIP_GENERICS("stripgenerics", Boolean.class, false),
	OUTPUT_SRC("outputsrc", Boolean.class, true);

	public static final TaskParameter[] VALUES = TaskParameter.values();

	public final String name;
	public final Class<?> type;
	public final Object defaultValue;

	TaskParameter(String name, Class<?> c, Object value) {
		TaskParameterMap.nameToParamMap.put(name, this);
		this.name = name;
		this.type = c;
		this.defaultValue = value;
	}

	public String getDesc() {
		String s = "task.param." + name;
		if (MCP.TRANSLATOR.hasKey(s)) {
			return MCP.TRANSLATOR.translateKey(s);
		}
		return MCP.TRANSLATOR.translateKey("task.noDesc");
	}

	private static String getDefaultFFOptions() {
		Map<String, Object> ffOptions = new HashMap<>(IFernflowerPreferences.DEFAULTS);
		ffOptions.put(IFernflowerPreferences.NO_COMMENT_OUTPUT, "1");
		ffOptions.put(IFernflowerPreferences.REMOVE_BRIDGE, "0");
		ffOptions.put(IFernflowerPreferences.ASCII_STRING_CHARACTERS, "1");
		ffOptions.put(IFernflowerPreferences.OVERRIDE_ANNOTATION, "0");
		ffOptions.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
		ffOptions.put(IFernflowerPreferences.INDENT_STRING, "\t");
		ffOptions.remove(IFernflowerPreferences.BANNER);
		return ffOptions.toString();
	}
}
