package org.mcphackers.mcp.tools.constants;

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

public abstract class Constants {
	
	public void replace(Path src) throws IOException {
		Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				String code = new String(Files.readAllBytes(file));
				code = replace_constants(code);
				Files.write(file, code.getBytes());
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	protected abstract String replace_constants(String code);

	public String replaceTextOfMatchGroup(String sourceString, Pattern pattern, Function<MatchResult,String> replaceStrategy) {
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
}
