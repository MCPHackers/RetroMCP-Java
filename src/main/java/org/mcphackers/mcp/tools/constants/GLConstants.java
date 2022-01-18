package org.mcphackers.mcp.tools.constants;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.tools.Util;

public class GLConstants extends Constants {
	
	private final JSONObject json;
	
	private final List _PACKAGES;
	private final Pattern _CALL_REGEX;
	private final Pattern _CONSTANT_REGEX;
	private final Pattern _INPUT_REGEX;
	private final Pattern _IMPORT;
	private final Map _CONSTANTS_KEYBOARD;
	private final List _CONSTANTS;
    
	public GLConstants() throws JSONException, IOException {
		json = getJson();
		
		_PACKAGES = json.getJSONArray("PACKAGES").toList();
		_CALL_REGEX = Pattern.compile("(" + String.join("|", _PACKAGES) + ")\\.([\\w]+)\\(.+?\\)");
		_CONSTANT_REGEX = Pattern.compile("(?<![-.\\w])\\d+(?![.\\w])");
		_INPUT_REGEX = Pattern.compile("((Keyboard)\\.((getKeyName|isKeyDown)\\(.+?\\)|getEventKey\\(\\) == .+?(?=[\\);]))|new KeyBinding\\([ \\w\\\"]+, .+?\\))");
		_IMPORT = Pattern.compile("import [.*\\w]+;");
		_CONSTANTS_KEYBOARD = Util.jsonToMap(json.getJSONObject("CONSTANTS_KEYBOARD"));
		_CONSTANTS = Util.jsonToList(json.getJSONArray("CONSTANTS"));
	}
	
    private String updateImport(String code, String imp) {
	    Matcher matcher = _IMPORT.matcher(code);
    	int lastIndex = -1;
	    while (matcher.find()) {
	        lastIndex = matcher.end();
	    }
	    String impString = "import " + imp + ";";
	    if(lastIndex >= 0 && !code.contains(impString)) {
	    	code = code.substring(0, lastIndex) + System.lineSeparator() + impString + code.substring(lastIndex);
	    }
        return code;
	}

	protected String replace_constants(String code) {
		Set<String> imports = new HashSet<String>();
		code = replaceTextOfMatchGroup(code, _INPUT_REGEX, match1 -> {
			String full_call = match1.group(0);
			return replaceTextOfMatchGroup(full_call, _CONSTANT_REGEX, match2 -> {
				String replaceConst = (String)_CONSTANTS_KEYBOARD.get(match2.group(0));
				if(replaceConst == null) {
					return match2.group();
				}
        		imports.add("org.lwjgl.input.Keyboard");
				return "Keyboard." + replaceConst;
			});
		});
		code = replaceTextOfMatchGroup(code, _CALL_REGEX, match1 -> {
			String full_call = match1.group(0);
			String pkg = match1.group(1);
			String method = match1.group(2);
			return replaceTextOfMatchGroup(full_call, _CONSTANT_REGEX, match2 -> {
	            String full_match = match2.group(0);
                for (Object groupg : _CONSTANTS) {
                	List group = (List)groupg;
                    if (((Map<String, List>)group.get(0)).containsKey(pkg) && ((List<Map<String, List>>)group).get(0).get(pkg).contains(method)) {
                        for (Entry entry : ((Map<String, List<String>>)group.get(1)).entrySet()) {
                            if(((Map)entry.getValue()).containsKey(full_match)) {
        	            		imports.add("org.lwjgl.opengl." + entry.getKey());
                                return String.format("%s.%s", entry.getKey(), ((Map)entry.getValue()).get(full_match));
                            }
                        }
                    }
                }
                return full_match;
			});
		});
		for(String imp : imports) {
			code = updateImport(code, imp);
		}
		return code;
	}
	
	private static JSONObject getJson() throws JSONException, IOException {
		return Util.parseJSONFile(GLConstants.class.getClassLoader().getResourceAsStream("gl_constants.json"));
	}
}
