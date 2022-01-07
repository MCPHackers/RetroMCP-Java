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
	private static final Pattern _INPUT_REGEX = Pattern.compile("(Keyboard)\\.((getKeyName|isKeyDown)\\(.+\\)|getEventKey\\(\\) == .+)");;
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

			private String replace_constants(String code) {
				//FIXME Still captures inccorrect matches, such as
				// "Keyboard.isKeyDown(42) && !Keyboard.isKeyDown(54) ? 1 : -1)"
				// Normal order would probably fix it, but it would also break indexes of match start and end
				code = replaceTextOfMatchGroup(code, _INPUT_REGEX, 0, match1 -> {
					return replaceTextOfMatchGroup(match1.group(0), _CONSTANT_REGEX, 0, match2 -> {
						String replaceConst = (String)_CONSTANTS_KEYBOARD.get(match2.group(0));
						if(replaceConst == null) {
							return match2.group();
						}
						return "Keyboard." + replaceConst;
					});
				});
				code = replaceTextOfMatchGroup(code, _CALL_REGEX, 0, match1 -> {
					String pkg = match1.group(1);
					String method = match1.group(2);
					return replaceTextOfMatchGroup(match1.group(0), _CONSTANT_REGEX, 0, match2 -> {
			            String full_match = match2.group(0);
	                    for (Object groupg : _CONSTANTS) {
	                    	List<Object> group = (List<Object>)groupg;
	                        if (((Map)group.get(0)).containsKey(pkg) && ((List)((Map)((List)group).get(0)).get(pkg)).contains(method)) {
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
				return code;
			}
        });
	}
	
	public static String replaceTextOfMatchGroup(String sourceString, Pattern pattern, int groupToReplace, Function<MatchResult,String> replaceStrategy) {
	    Stack<MatchResult> startPositions = new Stack<>();
	    Matcher matcher = pattern.matcher(sourceString);

	    while (matcher.find()) {
	        startPositions.push(matcher.toMatchResult());
	    }
	    StringBuilder sb = new StringBuilder(sourceString);
	    while (! startPositions.isEmpty()) {
	        MatchResult match = startPositions.pop();
	        if (match.start(groupToReplace) >= 0 && match.end(groupToReplace) >= 0) {
	            sb.replace(match.start(groupToReplace), match.end(groupToReplace), replaceStrategy.apply(match));
	        }
	    }
	    return sb.toString();       
	}
	
	private static JSONObject getJson() {
		try {
			return Util.parseJSONFile(Paths.get(GLConstants.class.getClassLoader().getResource("gl_constants.json").toURI()));
		} catch (JSONException | IOException | URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}
