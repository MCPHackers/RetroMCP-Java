package org.mcphackers.mcp.tasks.mode;

import java.util.HashMap;
import java.util.Map;

public class TaskParameterMap {
	/**
	 * Cached task parameter types
	 */
	static final Map<String, TaskParameter> nameToParamMap = new HashMap<>();
	
	public static TaskParameter get(String param) {
		return nameToParamMap.get(param);
	}

}
