package org.mcphackers.mcp.tools.mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MappingUtil {

	/**
	 * @param number
	 * @return Obfuscated string based on number index
	 */
	public static String getObfuscatedName(int number) {
		// Default obfuscation scheme
		return getObfuscatedName('a', 'z', number);
	}

	/**
	 * @param from
	 * @param to
	 * @param number
	 * @return Obfuscated string based on number index and character range
	 */
	public static String getObfuscatedName(char from, char to, int number) {
		if(number == 0) {
			return String.valueOf(from);
		}
		int num = number;
		int allChars = to - from  + 1;
		StringBuilder retName = new StringBuilder();
		while(num >= 0) {
			char c = (char) (from + (num % allChars));
			retName.insert(0, c);
			num = num / allChars - 1;
		}
		return retName.toString();
	}
	
	// Only works with tiny v2
	public static List<String> readNamespaces(Path mappings) throws IOException {
		List<String> namespaces = new ArrayList<>();
		try(BufferedReader reader = Files.newBufferedReader(mappings)) {
			String header = reader.readLine();
			if(header != null && header.startsWith("tiny\t2\t0\t")) {
				for(String namespace : header.substring(9).trim().split("\t")) {
					namespaces.add(namespace);
				}
			}
		}
		return namespaces;
	}

}
