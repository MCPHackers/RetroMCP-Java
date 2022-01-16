package org.mcphackers.mcp.tools;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.HashSet;
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
	private static final Pattern _IMPORT = Pattern.compile("import [.*\\w]+;");
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

			private String replace_constants(String code) {
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
