package org.mcphackers.mcp.tools.versions.json;

import org.json.JSONArray;

public final class JSONUtil {

	public static Object[] getArray(JSONArray array) {
		Object[] objs = new Object[array.length()];
		for(int i = 0; i < array.length(); i++) {
			objs[i] = array.get(i);
		}
		return objs;
	}
}
