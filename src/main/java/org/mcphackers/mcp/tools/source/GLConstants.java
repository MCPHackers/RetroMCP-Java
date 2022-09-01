package org.mcphackers.mcp.tools.source;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.tools.Util;

public class GLConstants extends Constants {

	private static final boolean COMMENT = true;
	private static Exception cause;
	private static final JSONObject json = getJson();
	private static final List<String> PACKAGES = json == null ? Collections.emptyList() : toList(json.getJSONArray("PACKAGES"));
	private static final JSONArray CONSTANTS = json == null ? null : json.getJSONArray("CONSTANTS");
	private static final JSONObject CONSTANTS_KEYBOARD = json == null ? null : json.getJSONObject("CONSTANTS_KEYBOARD");
	private static final Pattern CALL_REGEX = Pattern.compile("(" + String.join("|", PACKAGES) + ")\\.([\\w]+)\\(.+?\\)");
	private static final Pattern CONSTANT_REGEX = Pattern.compile("(?<![-.\\w])\\d+(?![.\\w])");
	private static final Pattern INPUT_REGEX = Pattern.compile("((Keyboard)\\.((getKeyName|isKeyDown)\\(.+?\\)|getEventKey\\(\\) == .+?(?=[);]))|new KeyBinding\\([ \\w\"]+, .+?\\))");
	
	private static JSONObject getJson() {
		try {
			return Util.parseJSON(GLConstants.class.getClassLoader().getResourceAsStream("gl_constants.json"));
		} catch (JSONException | IOException e) {
			cause = e;
			return null;
		}
	}
	
	private static List<String> toList(JSONArray packages) {
		if(packages == null) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>();
		for(int i = 0; i < packages.length(); i++) {
			list.add(packages.optString(i));
		}
		return list;
	}

	protected void replace_constants(StringBuilder source) {
		if (cause != null) {
			cause.printStackTrace();
			return;
		}
		Set<String> imports = new HashSet<>();
		replaceTextOfMatchGroup(source, INPUT_REGEX, match1 -> {
			String full_call = match1.group(0);
			return replaceTextOfMatchGroup(full_call, CONSTANT_REGEX, match2 -> {
				String replaceConst = CONSTANTS_KEYBOARD.optString(match2.group(0), null);
				if(replaceConst == null) {
					return match2.group();
				}
				imports.add("org.lwjgl.input.Keyboard");
				String constant = "Keyboard." + replaceConst;
				return COMMENT ? match2.group() + " /*" + constant + "*/"  : constant;
			});
		});
		replaceTextOfMatchGroup(source, CALL_REGEX, match1 -> {
			String full_call = match1.group(0);
			String pkg = match1.group(1);
			String method = match1.group(2);
			return replaceTextOfMatchGroup(full_call, CONSTANT_REGEX, match2 -> {
				String full_match = match2.group(0);
				for (Object groupg : CONSTANTS) {
					if(!(groupg instanceof JSONArray)) {
						continue;
					}
					JSONArray group = (JSONArray)groupg;
					JSONObject jsonObj1 = group.getJSONObject(0);
					if (jsonObj1.has(pkg) && jsonObj1.getJSONArray(pkg).toList().contains(method)) {
						JSONObject jsonObj = group.getJSONObject(1);
						Iterator<String> keys = jsonObj.keys();
						while(keys.hasNext()) {
							String key = keys.next();
							JSONObject value = jsonObj.getJSONObject(key);
							if(value.has(full_match)) {
								imports.add("org.lwjgl.opengl." + key);
								String[] constants = value.getString(full_match).split("\\|");
								for(int i = 0; i < constants.length; i++) {
									constants[i] = key + '.' + constants[i].trim();
								}
								String constant = String.join(" | ", constants);
								return COMMENT ? full_match + " /*" + constant + "*/"  : constant;
							}
						}
					}
				}
				return full_match;
			});
		});
		if(!COMMENT) updateImports(source, imports);
	}
}
