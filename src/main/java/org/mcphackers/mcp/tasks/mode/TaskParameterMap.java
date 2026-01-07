package org.mcphackers.mcp.tasks.mode;

import java.util.HashMap;
import java.util.Map;

public class TaskParameterMap {
	/**
	 * Cached task parameter types
	 */
	static final Map<String, TaskParameter> nameToParamMap = new HashMap<>();

	/**
	 * @param param Parameter name to be read from the cache
	 * @return {@link TaskParameter} instance read from cache
	 */
	public static TaskParameter get(String param) {
		return nameToParamMap.get(param);
	}

}
