package org.mcphackers.mcp.tasks;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.format.Tiny2Writer;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.ProgressInfo;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.tiny.Remapper;
import org.objectweb.asm.ClassReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TaskReobfuscate extends Task {
	private final Map<String, String> recompHashes = new HashMap<>();
	private final Map<String, String> originalHashes = new HashMap<>();

	public static final MemoryMappingTree mappingTree = new MemoryMappingTree();

	private final Map<String, String> reobfPackages = new HashMap<>();

	private final TaskUpdateMD5 md5Task = new TaskUpdateMD5(side, info);

	public TaskReobfuscate(int side, TaskInfo info) {
		super(side, info);
	}

	@Override
	public void doTask() throws Exception {

		Path reobfJar = Paths.get(chooseFromSide(MCPConfig.CLIENT_REOBF_JAR, MCPConfig.SERVER_REOBF_JAR));
		Path reobfBin = Paths.get(chooseFromSide(MCPConfig.CLIENT_BIN, MCPConfig.SERVER_BIN));
		Path reobfDir = Paths.get(chooseFromSide(MCPConfig.CLIENT_REOBF, MCPConfig.SERVER_REOBF));
		Path mappings = Paths.get(chooseFromSide(MCPConfig.CLIENT_MAPPINGS_RO, MCPConfig.SERVER_MAPPINGS_RO));

		step();
		md5Task.updateMD5(true);

		if (Files.exists(reobfBin)) {
			FileUtil.deleteDirectoryIfExists(reobfDir);
			step();
			gatherMD5Hashes(true);
			gatherMD5Hashes(false);

			step();
			readDeobfuscationMappings();
			writeReobfuscationMappings();

			Files.deleteIfExists(reobfJar);
			Remapper.remap(mappings, reobfBin, reobfJar, TaskDecompile.getLibraryPaths(side));
			step();
			unpack(reobfJar, reobfDir);
		} else {
			throw new IOException(chooseFromSide("Client", "Server") + " classes not found!");
		}
	}

	@Override
	public ProgressInfo getProgress() {
		int total = 100;
		int current;
		switch (step) {
			case 1: {
				current = 1;
				ProgressInfo info = md5Task.getProgress();
				int percent = info.getCurrent() / info.getTotal() * 50;
				return new ProgressInfo(info.getMessage(), current + percent, total);
			}
			case 2:
				current = 51;
				return new ProgressInfo("Gathering MD5 hashes...", current, total);
			case 3:
				current = 52;
				return new ProgressInfo("Reobfuscating...", current, total);
			case 4:
				current = 54;
				return new ProgressInfo("Unpacking...", current, total);
			default:
				return super.getProgress();
		}
	}

	// Utility methods
	private void writeReobfuscationMappings() throws IOException {

		Path reobfBin = Paths.get(chooseFromSide(MCPConfig.CLIENT_BIN, MCPConfig.SERVER_BIN));
		Path mappings = Paths.get(chooseFromSide(MCPConfig.CLIENT_MAPPINGS_RO, MCPConfig.SERVER_MAPPINGS_RO));

		do {
			if (mappingTree.visitHeader()) mappingTree.visitNamespaces("named", Collections.singletonList("official"));

			if (mappingTree.visitContent()) {
				Files.walkFileTree(reobfBin, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.toString().endsWith(".class") && !file.toString().endsWith("Start.class")) {
							ClassReader classReader = new ClassReader(Files.readAllBytes(file));
							String className = classReader.getClassName();
							if (mappingTree.visitClass(className)) {
								String packageName = className.lastIndexOf("/") >= 0 ? className.substring(0, className.lastIndexOf("/") + 1) : null;
								String obfPackage = reobfPackages.get(packageName);
								if(obfPackage == null) {
									obfPackage = "";
								}
								String clsName = className.lastIndexOf("/") >= 0 ? className.substring(className.lastIndexOf("/") + 1) : className;
								mappingTree.visitDstName(MappedElementKind.CLASS, 0, obfPackage + clsName);

								if (mappingTree.visitElementContent(MappedElementKind.CLASS)) {
									// could do members or class comment here
								}
							}
						}
						return super.visitFile(file, attrs);
					}
				});
			}
		} while (!mappingTree.visitEnd());

		try (Tiny2Writer writer = new Tiny2Writer(Files.newBufferedWriter(mappings), false)) {
			mappingTree.accept(writer);
		}
	}

	private void readDeobfuscationMappings() throws IOException {
		Path mappings = Paths.get(chooseFromSide(MCPConfig.CLIENT_MAPPINGS, MCPConfig.SERVER_MAPPINGS));

		try (BufferedReader reader = Files.newBufferedReader(mappings)) {
			Tiny2Reader.read(reader, mappingTree);
		}
	}

	private void gatherMD5Hashes(boolean reobf) throws IOException {
		Path md5 = Paths.get(reobf ? chooseFromSide(MCPConfig.CLIENT_MD5_RO, MCPConfig.SERVER_MD5_RO)
								   : chooseFromSide(MCPConfig.CLIENT_MD5, MCPConfig.SERVER_MD5));

		try (BufferedReader reader = Files.newBufferedReader(md5)) {
			String line = reader.readLine();
			while (line != null) {
				String[] tokens = line.split(" ");
				if (reobf) {
					recompHashes.put(tokens[0], tokens[1]);
				} else {
					originalHashes.put(tokens[0], tokens[1]);
				}

				// Read next line
				line = reader.readLine();
			}
		}
	}

	private void unpack(final Path src, final Path destDir) throws IOException {
		Map<String, String> reobfClasses = new HashMap<>();
		((MappingTree) mappingTree).getClasses().forEach(classEntry -> {
			reobfClasses.put(classEntry.getName("named"), classEntry.getDstName(0));
		});
		FileUtil.unzip(src, destDir, entry -> {
			String fileName = entry.getName();
			String deobfName = Util.getKey(reobfClasses, fileName.replace(".class", ""));
			String hash = originalHashes.get(deobfName);
			return !entry.isDirectory() && (hash == null || !hash.equals(recompHashes.get(deobfName)));
		});
	}
}
