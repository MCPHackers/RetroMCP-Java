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
import java.util.Objects;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.nio.MappingsIO;

import static org.mcphackers.mcp.MCPPaths.MAPPINGS;

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
		MappingNsRenamer nsRenamer = new MappingNsRenamer(Objects.requireNonNull(MappingWriter.create(out, MappingFormat.TINY_2_FILE)), nsRenames);
		MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsRenamer, currentDst);
		MappingReader.read(client, MappingFormat.TINY_2_FILE, nsSwitch);
	}

	// Reobfuscation utilities:
	public static Map<String, String> getPackageMappings(Map<String, String> classMappings) {
		Map<String, String> packageMappings = new HashMap<>();
		for (Map.Entry<String, String> entry : classMappings.entrySet()) {
			int i1 = entry.getKey().lastIndexOf('/');
			int i2 = entry.getValue().lastIndexOf('/');
			String name1 = i1 == -1 ? "" : entry.getKey().substring(0, i1 + 1);
			String name2 = i2 == -1 ? "" : entry.getKey().substring(0, i2 + 1);
			packageMappings.put(name1, name2);
		}
		return packageMappings;
	}

	public static Mappings getMappings(MCP mcp, ClassStorage storage, Task.Side side) throws IOException {
		Path mappingsPath = MCPPaths.get(mcp, MAPPINGS);
		if (!Files.exists(mappingsPath)) {
			return new Mappings();
		}
		final boolean enableObfuscation = mcp.getOptions().getBooleanParameter(TaskParameter.OBFUSCATION);
		final boolean srgObfuscation = mcp.getOptions().getBooleanParameter(TaskParameter.SRG_OBFUSCATION);
		List<String> nss = MappingUtil.readNamespaces(mappingsPath);
		boolean joined = srgObfuscation ? nss.contains("searge") : nss.contains("official");
		Mappings mappings = MappingsIO.read(mappingsPath, "named",
				joined ? (srgObfuscation ? "searge" : "official") : side.name);
		modifyClassMappings(mappings, storage.getAllClasses(), enableObfuscation);
		return mappings;
	}

	public static void modifyClassMappings(Mappings mappings, List<String> classNames, boolean obf) {
		Map<String, Integer> obfIndexes = new HashMap<>();
		Map<String, String> packageMappings = getPackageMappings(mappings.classes);
		for (String className : classNames) {
			String reobfName = mappings.classes.get(className);
			if (className.equals("net/minecraft/src/GuiMainMenu")) {
				System.out.println();
			}
			if (reobfName == null /*&& !hashes.containsKey(className)*/) {
				int i1 = className.lastIndexOf('/');
				String packageName = i1 == -1 ? "" : className.substring(0, i1 + 1);
				String obfPackage = packageMappings.get(packageName);
				String clsName = i1 == -1 ? className : className.substring(i1 + 1);
				if (obf) {
					int obfIndex = obfIndexes.getOrDefault(obfPackage, 0);
					String obfName = MappingUtil.getObfuscatedName(obfIndex);
					List<String> obfNames = new ArrayList<>();
					for (Map.Entry<String, String> entry : mappings.classes.entrySet()) {
						obfNames.add(entry.getValue());
					}
					while (obfNames.contains(obfPackage + obfName)) {
						obfIndex++;
						obfName = MappingUtil.getObfuscatedName(obfIndex);
					}
					if (obfIndex > obfIndexes.getOrDefault(obfPackage, 0)) {
						obfIndexes.put(obfPackage, obfIndex);
					}
					clsName = obfName;
				}
				if (obf || obfPackage != null) {
					String className2 = (obfPackage == null ? packageName : obfPackage) + clsName;
					mappings.classes.put(className, className2);
				}
			}
		}
	}

}
