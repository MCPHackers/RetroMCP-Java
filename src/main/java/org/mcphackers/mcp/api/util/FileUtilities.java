package org.mcphackers.mcp.api.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtilities {
	public static List<Path> getPathsOfType(Path startDirectory, String... types) {
		if (!Files.isDirectory(startDirectory)) {
			return Collections.emptyList();
		}

		try (Stream<Path> pathStream = Files.walk(startDirectory).parallel().filter(path -> Arrays.stream(types).anyMatch(type -> path.getFileName().toString().endsWith(type)))) {
			return pathStream.collect(Collectors.toList());
		} catch (IOException ex) {
			ex.printStackTrace();
			return Collections.emptyList();
		}
	}
}
