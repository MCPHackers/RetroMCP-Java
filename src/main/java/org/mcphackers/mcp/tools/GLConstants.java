package org.mcphackers.mcp.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class GLConstants {
	
	private static JSONObject json;
	
	private static final String[] _PACKAGES;
	private static final Pattern _CALL_REGEX;
	private static final Pattern _CONSTANT_REGEX;
	private static final Pattern _INPUT_REGEX;
	
	private static Map _CONSTANTS_KEYBOARD = new HashMap();
	private static List _CONSTANTS = new ArrayList();
	
	static {
		try {
			json = Util.parseJSONFile(Paths.get(GLConstants.class.getClassLoader().getResource("gl_constants.json").toURI()));
		} catch (JSONException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		
		List list = json.getJSONArray("PACKAGES").toList();
		_PACKAGES = (String[])list.toArray(new String[list.size()]);
		_CALL_REGEX = Pattern.compile("(" + String.join("|", _PACKAGES) + ")\\.([\\w]+)\\(.+\\)");
		_CONSTANT_REGEX = Pattern.compile("(?<![-.\\w])\\d+(?![.\\w])");
		_INPUT_REGEX = Pattern.compile("(Keyboard)\\.((getKeyName|isKeyDown)\\(.+\\)|getEventKey\\(\\) == .+)");
		setConstants();
	}
	
	public static void annotate(Path src) {
	}

	private static void setConstants() {
		_CONSTANTS = Util.jsonToList(json.getJSONArray("CONSTANTS"));
		_CONSTANTS_KEYBOARD = Util.jsonToMap(json.getJSONObject("CONSTANTS_KEYBOARD"));
	}
}
