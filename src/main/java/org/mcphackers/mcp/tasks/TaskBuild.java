package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.nio.ClassStorageWriter;
import org.mcphackers.rdi.nio.MappingsIO;
import org.mcphackers.rdi.nio.RDInjector;
import org.objectweb.asm.ClassWriter;

public class TaskBuild extends TaskStaged {
	/*
	 * Indexes of stages for plugin overrides
	 */
	public static final int STAGE_RECOMPILE = 0;
	public static final int STAGE_REOBF = 1;
	public static final int STAGE_BUILD = 2;

	public TaskBuild(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	protected Stage[] setStages() {
		Path bin = MCPPaths.get(mcp, BIN, side);
		return new Stage[]{stage(getLocalizedStage("recompile", 0), () -> new TaskRecompile(side, mcp, this).doTask()), stage(getLocalizedStage("gathermd5", 25), () -> new TaskUpdateMD5(side, mcp, this).updateMD5(true)), stage(getLocalizedStage("reobf"), 50, this::reobfuscate)};
	}

	@Override
	public void setProgress(int progress) {
		switch (step) {
			case STAGE_RECOMPILE: {
				int percent = (int) (progress * 0.49D);
				super.setProgress(1 + percent);
				break;
			}
			case STAGE_REOBF: {
				int percent = (int) (progress * 0.20D);
				super.setProgress(50 + percent);
				break;
			}
			default:
				super.setProgress(progress);
				break;
		}
	}

	// Reobfuscation utilities
	private Mappings getMappings(ClassStorage storage, Side side) throws IOException {
		Path mappingsPath = MCPPaths.get(mcp, MAPPINGS);
		if (!Files.exists(mappingsPath)) {
			return new Mappings();
		}
		final boolean enableObfuscation = mcp.getOptions().getBooleanParameter(TaskParameter.OBFUSCATION);
		boolean joined = MappingUtil.readNamespaces(mappingsPath).contains("official");
		Mappings mappings = MappingsIO.read(mappingsPath, "named", joined ? "official" : side.name);
		modifyClassMappings(mappings, storage.getAllClasses(), enableObfuscation);
		return mappings;
	}

	private void modifyClassMappings(Mappings mappings, List<String> classNames, boolean obf) {
		Map<String, Integer> obfIndexes = new HashMap<>();
		Map<String, String> packageMappings = getPackageMappings(mappings.classes);
		for (String className : classNames) {
			String reobfName = mappings.classes.get(className);
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

	private Map<String, String> gatherMD5Hashes(boolean reobf) throws IOException {
		final Path md5 = MCPPaths.get(mcp, reobf ? MCPPaths.MD5_RO : MCPPaths.MD5, side);
		Map<String, String> hashes = new HashMap<>();

		try (Stream<String> lines = Files.lines(md5)) {
			lines.forEach((line) -> {
				String[] tokens = line.split(" ");
				hashes.put(tokens[0], tokens[1]);
			});
		}
		return hashes;
	}

	private static Map<String, String> getPackageMappings(Map<String, String> classMappings) {
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

	private void reobfuscate() throws IOException {
		final Path reobfBin = MCPPaths.get(mcp, BIN, side);

		Side[] sides = side == Side.MERGED ? new Side[]{Side.CLIENT, Side.SERVER} : new Side[]{side};

		Map<String, String> originalHashes = gatherMD5Hashes(false);
		Map<String, String> recompHashes = gatherMD5Hashes(true);

		for (Side localSide : sides) {
			final Path reobfJar = MCPPaths.get(mcp, REOBF_JAR, localSide);
			Files.deleteIfExists(reobfJar);
			RDInjector injector = new RDInjector(reobfBin);
			Mappings mappings = getMappings(injector.getStorage(), localSide);
			if (mappings != null) {
				injector.applyMappings(mappings);
			}
			injector.transform();
			new ClassStorageWriter(injector.getStorage(), ClassWriter.COMPUTE_MAXS).write(Files.newOutputStream(reobfJar));

			Map<String, String> reversedNames = new HashMap<>();
			if (mappings != null) {
				for (Map.Entry<String, String> entry : mappings.classes.entrySet()) {
					reversedNames.put(entry.getValue(), entry.getKey());
				}
			}
			Pattern regexPattern = Pattern.compile(mcp.getOptions().getStringParameter(TaskParameter.EXCLUDED_CLASSES));
			List<Path> changedFiles = new ArrayList<>();
			List<Path> assets = new ArrayList<>(FileUtil.walkDirectory(reobfBin, p -> !Files.isDirectory(p) && !p.toString().endsWith(".class")));

			// Identify differences
			try (FileSystem fs = FileSystems.newFileSystem(reobfJar, null)) {
				Iterable<Path> rootDirectories = fs.getRootDirectories();
				for (Path root : rootDirectories) {
					try (Stream<Path> stream = Files.walk(root)) {
						stream.forEach(path -> {
							if (!Files.isDirectory(path)) {
								String obfClassName = root.relativize(path).toString().replace(".class", "");
								// Force inner classes to compare outer class hash
								String className = obfClassName;
								int index = className.indexOf('$');
								if (index != -1) {
									className = className.substring(0, index);
								}
								String deobfName = reversedNames.get(className);
								if (deobfName == null) {
									deobfName = className;
								}
								String hash = originalHashes.get(deobfName);
								String hashModified = recompHashes.get(deobfName);
								boolean extract = (hash == null) || !hash.equals(hashModified) && !regexPattern.matcher(deobfName).matches();
								if (extract) {
									changedFiles.add(path);
									System.out.println(reversedNames.get(obfClassName) + " : " + obfClassName);
								}
							}
						});
					}
				}
			}

			// Export differences to JAR/ZIP file
			Path buildJar = MCPPaths.get(mcp, BUILD_JAR, localSide);
			Path buildZip = MCPPaths.get(mcp, BUILD_ZIP, localSide);
			try {
				FileUtil.createDirectories(MCPPaths.get(mcp, BUILD));
				// Simply copy re-obfuscated JAR
				if (mcp.getOptions().getBooleanParameter(TaskParameter.FULL_BUILD)) {
					Files.deleteIfExists(buildJar);
					Files.copy(reobfJar, buildJar);
					try (FileSystem fs = FileSystems.newFileSystem(buildJar, null)) {
						// Copy assets
						for (Path asset : assets) {
							Files.copy(Files.newInputStream(asset), fs.getPath(reobfBin.relativize(asset).toString()));
						}
					}
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.DSA");
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.SF");
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/CODESIGN.DSA");
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/CODESIGN.RSA");
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/CODESIGN.SF");
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANGCS.RSA");
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANGCS.SF");
				} else {
					Files.deleteIfExists(buildZip);

					Map<String, String> env = new HashMap<>();
					env.put("create", "true");

					URI buildZipURI = URI.create("jar:file:" + buildZip.toUri().getPath());

					try (FileSystem reobfFs = FileSystems.newFileSystem(reobfJar, null)) {
						try (FileSystem buildFs = FileSystems.newFileSystem(buildZipURI, env, null)) {
							// Copy different files
							for (Path changedFile : changedFiles) {
								Path file = reobfFs.getPath(changedFile.toString());
								Files.copy(file, buildFs.getPath(changedFile.toString()));
							}

							// Copy assets
							for (Path asset : assets) {
								Files.copy(Files.newInputStream(asset), buildFs.getPath(reobfBin.relativize(asset).toString()));
							}
						}
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
