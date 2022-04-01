package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;

public class TaskBuild extends Task {

	private static final int REOBF = 1;
	private static final int BUILD = 2;
	private static final int STEPS = 2;

	public TaskBuild(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		Path originalJar =  Paths.get(chooseFromSide(MCPPaths.CLIENT, 			MCPPaths.SERVER));
		Path bin = 			Paths.get(chooseFromSide(MCPPaths.CLIENT_BIN, 		MCPPaths.SERVER_BIN));
		Path reobfDir = 	Paths.get(chooseFromSide(MCPPaths.CLIENT_REOBF, 	MCPPaths.SERVER_REOBF));
		Path buildJar = 	Paths.get(chooseFromSide(MCPPaths.BUILD_JAR_CLIENT, MCPPaths.BUILD_JAR_SERVER));
		Path buildZip = 	Paths.get(chooseFromSide(MCPPaths.BUILD_ZIP_CLIENT, MCPPaths.BUILD_ZIP_SERVER));
		
		while(step < STEPS) {
			step();
			switch (step) {
			case REOBF:
				new TaskReobfuscate(side, mcp, this).doTask();
				break;
			case BUILD:
				FileUtil.createDirectories(Paths.get(MCPPaths.BUILD));
				if(mcp.getOptions().getBooleanParameter(TaskParameter.FULL_BUILD)) {
					Files.deleteIfExists(buildJar);
					Files.copy(originalJar, buildJar);
					List<Path> reobfClasses = FileUtil.walkDirectory(reobfDir, path -> !Files.isDirectory(path));
					FileUtil.packFilesToZip(buildJar, reobfClasses, reobfDir);
					List<Path> assets = FileUtil.walkDirectory(bin, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class"));
					FileUtil.packFilesToZip(buildJar, assets, bin);
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.DSA");
					FileUtil.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.SF");
				}
				else {
					Files.deleteIfExists(buildZip);
					FileUtil.compress(reobfDir, buildZip);
					List<Path> assets = FileUtil.walkDirectory(bin, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class"));
					FileUtil.packFilesToZip(buildZip, assets, bin);
				}
				break;
			}
		}
	}

	@Override
	public String getName() {
		return "Build";
	}
	
	public void setProgress(int progress) {
		switch (step) {
		case 1: {
			int percent = (int)((double)progress * 0.49D);
			super.setProgress(1 + percent);
			break;
		}
		default:
			super.setProgress(progress);
			break;
		}
	}

	protected void updateProgress() {
		switch (step) {
		case REOBF:
			break;
		case BUILD:
			setProgress("Building...", 52);
			break;
		default:
			super.updateProgress();
			break;
		}
	}
}
