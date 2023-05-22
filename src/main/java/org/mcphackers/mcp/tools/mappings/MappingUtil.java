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

import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.format.Tiny2Writer;
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
		try (BufferedReader reader = Files.newBufferedReader(client)) {
			Tiny2Reader.read(reader, clientTree);
		}
		try (BufferedReader reader = Files.newBufferedReader(server)) {
			Tiny2Reader.read(reader, serverTree);
		}
		clientTree.setSrcNamespace("client");
		serverTree.setSrcNamespace("server");
		MemoryMappingTree namedClientTree = new MemoryMappingTree();
		{
			Map<String, String> namespaces = new HashMap<>();
			namespaces.put("named", "client");
			MappingNsCompleter nsCompleter = new MappingNsCompleter(namedClientTree, namespaces);
			MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsCompleter, "named");
			clientTree.accept(nsSwitch);
		}
		MemoryMappingTree namedServerTree = new MemoryMappingTree();
		{
			Map<String, String> namespaces = new HashMap<>();
			namespaces.put("named", "server");
			MappingNsCompleter nsCompleter = new MappingNsCompleter(namedServerTree, namespaces);
			MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsCompleter, "named");
			serverTree.accept(nsSwitch);
		}
		namedServerTree.accept(namedClientTree);
		try (Tiny2Writer writer = new Tiny2Writer(Files.newBufferedWriter(out), false)) {
			namedClientTree.accept(writer);
		}
	}

	//official named -> named client
	public static void mergeMappings(Path client, Path out) throws IOException {
		MemoryMappingTree clientTree = new MemoryMappingTree();
		try (BufferedReader reader = Files.newBufferedReader(client)) {
			Tiny2Reader.read(reader, clientTree);
		}
		clientTree.setSrcNamespace("client");
		MemoryMappingTree namedClientTree = new MemoryMappingTree();
		{
			Map<String, String> namespaces = new HashMap<>();
			namespaces.put("named", "client");
			MappingNsCompleter nsCompleter = new MappingNsCompleter(namedClientTree, namespaces);
			MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsCompleter, "named");
			clientTree.accept(nsSwitch);
		}
		try (Tiny2Writer writer = new Tiny2Writer(Files.newBufferedWriter(out), false)) {
			namedClientTree.accept(writer);
		}
	}

}
