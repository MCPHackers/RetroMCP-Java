package org.mcphackers.mcp.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class JSONUtil {

	public static Object[] getArray(JSONArray array) {
		Object[] objs = new Object[array.length()];
		for (int i = 0; i < array.length(); i++) {
			objs[i] = array.get(i);
		}
		return objs;
	}

	public static JSONObject getJSON(InputStream stream) {
		try {
			return parseJSON(stream);
		} catch (JSONException | IOException ex) {
			return null;
		}
	}

	public static List<String> toList(JSONArray array) {
		if (array == null) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			String s = array.optString(i, null);
			if (s == null) continue;
			list.add(s);
		}
		return list;
	}

	public static Map<Integer, String> toMap(JSONObject object) {
		if (object == null) {
			return Collections.emptyMap();
		}
		Map<Integer, String> map = new HashMap<>();
		for (String key : object.keySet()) {
			String value = object.optString(key, null);
			if (value == null) continue;
			try {
				int i = Integer.parseInt(key);
				map.put(i, value);
			} catch (NumberFormatException ignored) {
			}
		}
		return map;
	}

	public static JSONObject parseJSONFile(Path path) throws JSONException, IOException {
		String content = new String(Files.readAllBytes(path));
		return new JSONObject(content);
	}

	public static JSONObject parseJSON(InputStream stream) throws JSONException, IOException {
		byte[] bytes = Util.readAllBytes(stream);
		String content = new String(bytes);
		return new JSONObject(content);
	}

	public static JSONArray parseJSONArray(InputStream stream) throws JSONException, IOException {
		byte[] bytes = Util.readAllBytes(stream);
		String content = new String(bytes);
		return new JSONArray(content);
	}
}
