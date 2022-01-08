package org.mcphackers.mcp.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class GLConstants {
	
	private static final JSONObject json = getJson();
	
	private static final List _PACKAGES = json.getJSONArray("PACKAGES").toList();
	private static final Pattern _CALL_REGEX = Pattern.compile("(" + String.join("|", _PACKAGES) + ")\\.([\\w]+)\\(.+\\)");
	private static final Pattern _CONSTANT_REGEX = Pattern.compile("(?<![-.\\w])\\d+(?![.\\w])");
	private static final Pattern _INPUT_REGEX = Pattern.compile("((Keyboard)\\.((getKeyName|isKeyDown)\\(\\d+\\)|getEventKey\\(\\) == \\d+)|new KeyBinding\\([ \\w\\\"]+, \\d+\\))");
	private static final Map _CONSTANTS_KEYBOARD = Util.jsonToMap(json.getJSONObject("CONSTANTS_KEYBOARD"));
	private static final List _CONSTANTS = Util.jsonToList(json.getJSONArray("CONSTANTS"));
	
	public static void replace(Path src) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            	String code = new String(Files.readAllBytes(file));
            	code = replace_constants(code);
            	Files.write(file, code.getBytes());
                return FileVisitResult.CONTINUE;
            }
            
            private String updateImports(String code, String imp) {
            	// TODO Import won't be added if GL11 isn't imported already
                String addAfter = "org.lwjgl.opengl.GL11";
                if(!code.contains("import " + imp + ";")) {
                    code = code.replace("import " + addAfter + ";",
                                        "import " + addAfter + ";\nimport " + imp + ";");
                }
                return code;
        	}

			private String replace_constants(String code) {
				code = replaceTextOfMatchGroup(code, _INPUT_REGEX, match1 -> {
					String full_call = match1.group(0);
					return replaceTextOfMatchGroup(full_call, _CONSTANT_REGEX, match2 -> {
						String replaceConst = (String)_CONSTANTS_KEYBOARD.get(match2.group(0));
						if(replaceConst == null) {
							return match2.group();
						}
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
	                                    return String.format("%s.%s", entry.getKey(), ((Map)entry.getValue()).get(full_match));
	                                }
	                            }
	                        }
	                    }
	                    return full_match;
					});
				});
				for(String pkg : (List<String>)_PACKAGES) {
					if(code.contains(pkg + ".")) {
						code = updateImports(code, "org.lwjgl.opengl." + pkg);
					}
				}
				if(code.contains("Keyboard.")) {
					code = updateImports(code, "org.lwjgl.input.Keyboard");
				}
				return code;
			}
        });
	}
	
	public static String replaceTextOfMatchGroup(String sourceString, Pattern pattern, Function<MatchResult,String> replaceStrategy) {
	    Stack<MatchResult> startPositions = new Stack<>();
	    Matcher matcher = pattern.matcher(sourceString);

	    while (matcher.find()) {
	        startPositions.push(matcher.toMatchResult());
	    }
	    StringBuilder sb = new StringBuilder(sourceString);
	    while (! startPositions.isEmpty()) {
	        MatchResult match = startPositions.pop();
	        if (match.start() >= 0 && match.end() >= 0) {
	            sb.replace(match.start(), match.end(), replaceStrategy.apply(match));
	        }
	    }
	    return sb.toString();       
	}
	
	private static JSONObject getJson() {
		try {
			return Util.parseJSONFile(GLConstants.class.getClassLoader().getResourceAsStream("gl_constants.json"));
		} catch (JSONException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
