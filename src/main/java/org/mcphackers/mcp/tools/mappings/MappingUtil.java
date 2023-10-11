package org.mcphackers.mcp.tools.mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

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
		if (number == 0) {
			return String.valueOf(from);
		}
		int num = number;
		int allChars = to - from + 1;
		StringBuilder retName = new StringBuilder();
		while (num >= 0) {
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
		if (number == 0) {
			return String.valueOf(chars[0]);
		}
		int num = number;
		int allChars = chars.length;
		StringBuilder retName = new StringBuilder();
		while (num >= 0) {
			char c = chars[num % allChars];
			retName.insert(0, c);
			num = num / allChars - 1;
		}
		return retName.toString();
	}

	public static List<String> readNamespaces(Path mappings) throws IOException {
		List<String> namespaces = new ArrayList<>();
		boolean invalid = false;
		try (BufferedReader reader = Files.newBufferedReader(mappings)) {
			String header = reader.readLine();
			if (header != null) {
				if (header.startsWith("tiny\t2\t0\t")) {
					namespaces.addAll(Arrays.asList(header.substring(9).trim().split("\t")));
				} else if (header.startsWith("v1\t")) {
					namespaces.addAll(Arrays.asList(header.substring(3).trim().split("\t")));
				} else {
					invalid = true;
				}
			} else {
				invalid = true;
			}
		}
		if (invalid) {
			throw new IllegalStateException("No valid tiny header in " + mappings);
		}
		return namespaces;
	}

	//official named -> named client server
	public static void mergeMappings(Path client, Path server, Path out) throws IOException {
		MemoryMappingTree mergedMappingTree = new MemoryMappingTree();

		// Create visitors to flip the mapping tree
		Map<String, String> clientNsRenames = Collections.singletonMap("official", "client");
		Map<String, String> serverNsRenames = Collections.singletonMap("official", "server");

		MappingNsRenamer clientNsRenamer = new MappingNsRenamer(mergedMappingTree, clientNsRenames);
		MappingSourceNsSwitch clientNsSwitch = new MappingSourceNsSwitch(clientNsRenamer, "named");
		MappingNsRenamer serverNsRenamer = new MappingNsRenamer(mergedMappingTree, serverNsRenames);
		MappingSourceNsSwitch serverNsSwitch = new MappingSourceNsSwitch(serverNsRenamer, "named");

		// Read client/server mappings with the above visitors
		MappingReader.read(client, MappingFormat.TINY_2_FILE, clientNsSwitch);
		MappingReader.read(server, MappingFormat.TINY_2_FILE, serverNsSwitch);

		// Export the merged mapping trees
		mergedMappingTree.accept(MappingWriter.create(out, MappingFormat.TINY_2_FILE));
	}

	//official named -> named client
	public static void mergeMappings(Path client, Path out) throws IOException {
		String currentSrc = "official";
		String currentDst = "named";
		String newDst = "client";
		Map<String, String> nsRenames = new HashMap<>();
		nsRenames.put(currentSrc, newDst);

		// Switch the namespaces from official -> named to named -> official
		// Rename the namespaces from named -> official to named -> client
		MappingNsRenamer nsRenamer = new MappingNsRenamer(MappingWriter.create(out, MappingFormat.TINY_2_FILE), nsRenames);
		MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsRenamer, currentDst);
		MappingReader.read(client, MappingFormat.TINY_2_FILE, nsSwitch);
	}
}
