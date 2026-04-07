package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.injector.ExcludingStorageWriter;
import org.mcphackers.mcp.tools.injector.SourceFileTransformer;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.rdi.injector.data.Mappings;
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
		boolean fullBuild = mcp.getOptions().getBooleanParameter(TaskParameter.FULL_BUILD);

		return new Stage[]{
				stage(getLocalizedStage("recompile"), 0,
						() -> new TaskRecompile(side, mcp, this).doTask()),
				stage(getLocalizedStage("gathermd5"), 25,
						() -> new TaskUpdateMD5(side, mcp, this).updateMD5(true)),
				stage(getLocalizedStage("reobf"), 50, this::reobfuscate),
				stage(getLocalizedStage("build"), 70,
						() -> {
							Side[] sides = side == Side.MERGED ? new Side[]{Side.CLIENT, Side.SERVER} : new Side[]{side};
							for (Side localSide : sides) {
								Path originalJar = MCPPaths.get(mcp, JAR_ORIGINAL, localSide);
								Path reobfJar = MCPPaths.get(mcp, REOBF_JAR, localSide);
								FileUtil.createDirectories(MCPPaths.get(mcp, BUILD));
								Predicate<Path> walkFilter = path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class") && !path.getFileName().toString().endsWith(".DS_Store");

								if (fullBuild) {
									Path buildJar = MCPPaths.get(mcp, BUILD_JAR, localSide);

									Files.deleteIfExists(buildJar);
									Files.copy(originalJar, buildJar);
									FileUtil.copyZipContentsIntoZip(reobfJar, buildJar);
									List<Path> assets = FileUtil.walkDirectory(bin, walkFilter);
									FileUtil.packFilesToZip(buildJar, assets, bin);
									FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.DSA");
									FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.SF");
									FileUtil.deleteFileInAZip(buildJar, "/META-INF/CODESIGN.DSA");
									FileUtil.deleteFileInAZip(buildJar, "/META-INF/CODESIGN.SF");
								} else {
									Path buildZip = MCPPaths.get(mcp, BUILD_ZIP, localSide);

									Files.deleteIfExists(buildZip);
									Files.copy(reobfJar, buildZip);
									List<Path> assets = FileUtil.walkDirectory(bin, walkFilter);
									FileUtil.packFilesToZip(buildZip, assets, bin);
								}
							}
						})
		};
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

	private void reobfuscate() throws IOException {
		final Path reobfBin = MCPPaths.get(mcp, BIN, side);
		final boolean stripSourceFile = mcp.getOptions().getBooleanParameter(TaskParameter.STRIP_SOURCE_FILE);
		final boolean fullBuild = mcp.getOptions().getBooleanParameter(TaskParameter.FULL_BUILD);

		Side[] sides = side == Side.MERGED ? new Side[]{Side.CLIENT, Side.SERVER} : new Side[]{side};

		Map<String, String> originalHashes = Util.gatherMD5Hashes(mcp, side, false);
		Map<String, String> recompHashes = Util.gatherMD5Hashes(mcp, side,true);

		for (Side localSide : sides) {
			final Path reobfJar = MCPPaths.get(mcp, REOBF_JAR, localSide);
			if (!Files.exists(reobfJar.getParent())) {
				Files.createDirectories(reobfJar.getParent());
			}
			Files.deleteIfExists(reobfJar);
			RDInjector injector = new RDInjector(reobfBin);
			Mappings mappings = MappingUtil.getMappings(mcp, injector.getStorage(), localSide);
			if (mappings != null) {
				injector.applyMappings(mappings);
			}
			if (stripSourceFile) {
				injector.addTransform(SourceFileTransformer::removeSourceFileAttributes);
			}
			injector.transform();

			Map<String, String> reversedNames = new HashMap<>();
			if (mappings != null) {
				for (Map.Entry<String, String> entry : mappings.classes.entrySet()) {
					reversedNames.put(entry.getValue(), entry.getKey());
				}
			}
			Pattern regexPattern = Pattern.compile(mcp.getOptions().getStringParameter(TaskParameter.EXCLUDED_CLASSES));

			// Exclude all unchanged classes if full build is OFF
			Set<String> excludes = new HashSet<>();
			if (!fullBuild) {
				for (String className : injector.getStorage().getAllClasses()) {
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
					boolean extract = (hash == null) || !hash.equals(hashModified); //&& !regexPattern.matcher(deobfName).matches();
					if (!extract) {
						excludes.add(className);
					} else {
						System.out.println(reversedNames.get(className) + " : " + className);
					}
				}
			}

			new ExcludingStorageWriter(injector.getStorage(), ClassWriter.COMPUTE_MAXS, excludes).write(Files.newOutputStream(reobfJar));
		}
	}
}
