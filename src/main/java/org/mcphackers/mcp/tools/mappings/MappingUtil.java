package org.mcphackers.mcp.tools.mappings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
		MemoryMappingTree clientTree = new MemoryMappingTree();
		MemoryMappingTree serverTree = new MemoryMappingTree();
		MappingReader.read(client, MappingFormat.TINY_2_FILE, clientTree);
		MappingReader.read(server, MappingFormat.TINY_2_FILE, serverTree);
		clientTree.setSrcNamespace("client");
		serverTree.setSrcNamespace("server");
		MemoryMappingTree namedClientTree = new MemoryMappingTree();
		Map<String, String> clientNamespaces = new HashMap<>();
		clientNamespaces.put("named", "client");
		flipMappingTree(namedClientTree, clientNamespaces);
		MemoryMappingTree namedServerTree = new MemoryMappingTree();
		Map<String, String> serverNamespaces = new HashMap<>();
		serverNamespaces.put("named", "server");
		flipMappingTree(namedServerTree, serverNamespaces);
		namedServerTree.accept(namedClientTree);
		try (Tiny2FileWriter writer = new Tiny2FileWriter(Files.newBufferedWriter(out), false)) {
			namedClientTree.accept(writer);
		}
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

	public static void flipMappingTree(MemoryMappingTree mappingTree, Map<String, String> namespaces) throws IOException {
		MappingNsCompleter nsCompleter = new MappingNsCompleter(mappingTree, namespaces);
		MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsCompleter, "named");
		mappingTree.accept(nsSwitch);
	}
}
