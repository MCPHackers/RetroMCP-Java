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
	
	/**
	 * @param chars
	 * @param number
	 * @return Obfuscated string based on number index and character array
	 */
	public static String getObfuscatedName(char[] chars, int number) {
		if(number == 0) {
			return String.valueOf(chars[0]);
		}
		int num = number;
		int allChars = chars.length;
		StringBuilder retName = new StringBuilder();
		while(num >= 0) {
			char c = chars[num % allChars];
			retName.insert(0, c);
			num = num / allChars - 1;
		}
		return retName.toString();
	}
	
	public static List<String> readNamespaces(Path mappings) throws IOException {
		List<String> namespaces = new ArrayList<>();
		boolean invalid = false;
		try(BufferedReader reader = Files.newBufferedReader(mappings)) {
			String header = reader.readLine();
			if(header != null) {
				if(header.startsWith("tiny\t2\t0\t")) {
					for(String namespace : header.substring(9).trim().split("\t")) {
						namespaces.add(namespace);
					}
				}
				else if(header.startsWith("v1\t")) {
					for(String namespace : header.substring(3).trim().split("\t")) {
						namespaces.add(namespace);
					}
				}
				else {
					invalid  = true;
				}
			}
			else {
				invalid = true;
			}
		}
		if(invalid) {
			throw new IllegalStateException("No valid tiny header in " + mappings);
		}
		return namespaces;
	}

}
