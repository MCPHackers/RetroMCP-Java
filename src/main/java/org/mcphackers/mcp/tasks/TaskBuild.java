package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;

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
		return new Stage[]{
				stage(getLocalizedStage("recompile"),
						() -> new TaskRecompile(side, mcp, this).doTask()),
				stage(getLocalizedStage("reobf"), 50,
						() -> new TaskReobfuscate(side, mcp, this).doTask()),
				stage(getLocalizedStage("build"), 70,
						() -> {
							Side[] sides = side == Side.MERGED ? new Side[]{Side.CLIENT, Side.SERVER} : new Side[]{side};
							for (Side localSide : sides) {
								Path originalJar = MCPPaths.get(mcp, JAR_ORIGINAL, localSide);
								Path reobfDir = MCPPaths.get(mcp, REOBF_SIDE, localSide);
								Path buildJar = MCPPaths.get(mcp, BUILD_JAR, localSide);
								Path buildZip = MCPPaths.get(mcp, BUILD_ZIP, localSide);
								FileUtil.createDirectories(MCPPaths.get(mcp, BUILD));
								if (mcp.getOptions().getBooleanParameter(TaskParameter.FULL_BUILD)) {
									Files.deleteIfExists(buildJar);
									Files.copy(originalJar, buildJar);
									List<Path> reobfClasses = FileUtil.walkDirectory(reobfDir, path -> !Files.isDirectory(path));
									FileUtil.packFilesToZip(buildJar, reobfClasses, reobfDir);
									List<Path> assets = FileUtil.walkDirectory(bin, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class"));
									FileUtil.packFilesToZip(buildJar, assets, bin);
									FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.DSA");
									FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.SF");
									FileUtil.deleteFileInAZip(buildJar, "/META-INF/CODESIGN.DSA");
									FileUtil.deleteFileInAZip(buildJar, "/META-INF/CODESIGN.SF");
								} else {
									Files.deleteIfExists(buildZip);
									FileUtil.compress(reobfDir, buildZip);
									List<Path> assets = FileUtil.walkDirectory(bin, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class"));
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
}
