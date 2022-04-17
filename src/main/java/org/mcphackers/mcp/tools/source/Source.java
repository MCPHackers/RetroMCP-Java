package org.mcphackers.mcp.tools.source;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Source {
	
	public final static String[] validModifiers = {"public", "protected", "private", "abstract", "static", "final", "strictfp", "transient", "volatile", "synchronized", "native"};
	
	public static void modify(Path src, Function<String, String> editCode) throws IOException {
		Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				String code = new String(Files.readAllBytes(file));
				code = editCode.apply(code);
				Files.write(file, code.getBytes());
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	public static String replaceTextOfMatchGroup(String sourceString, Pattern pattern, Function<MatchResult,String> replaceStrategy) {
		Stack<MatchResult> results = new Stack<>();
		Matcher matcher = pattern.matcher(sourceString);

		while (matcher.find()) {
			results.push(matcher.toMatchResult());
		}
		StringBuilder sb = new StringBuilder(sourceString);
		while (!results.isEmpty()) {
			MatchResult match = results.pop();
			if (match.start() >= 0 && match.end() >= 0) {
				sb.replace(match.start(), match.end(), replaceStrategy.apply(match));
			}
		}
		return sb.toString();
	}
	
	public static String addAfterMatch(String sourceString, Pattern pattern, String stringToAdd) {
		return addMatch(sourceString, pattern, true, stringToAdd);
	}
	
	public static String addBeforeMatch(String sourceString, Pattern pattern, String stringToAdd) {
		return addMatch(sourceString, pattern, false, stringToAdd);
	}
	
	private static String addMatch(String sourceString, Pattern pattern, boolean after, String stringToAdd) {
		Stack<MatchResult> results = new Stack<>();
		Matcher matcher = pattern.matcher(sourceString);

		while (matcher.find()) {
			results.push(matcher.toMatchResult());
		}
		StringBuilder sb = new StringBuilder(sourceString);
		while (!results.isEmpty()) {
			MatchResult match = results.pop();
			if(after) {
				if (match.end() >= 0) {
					sb.insert(match.end(), stringToAdd);
				}
			}
			else {
				if (match.start() >= 0) {
					sb.insert(match.start(), stringToAdd);
				}
			}
		}
		return sb.toString();
	}
}
