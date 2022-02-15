package org.mcphackers.mcp.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import codechicken.diffpatch.cli.CliOperation;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.ProgressInfo;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.constants.GLConstants;
import org.mcphackers.mcp.tools.constants.MathConstants;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.mcinjector.MCInjector;
import org.mcphackers.mcp.tools.tiny.Remapper;
import codechicken.diffpatch.cli.PatchOperation;

public class TaskDecompile extends Task {

	private final Decompiler decompiler;
	private TaskUpdateMD5 md5Task;
	private TaskRecompile recompTask;
	
	private static final int REMAP = 1;
	private static final int EXCEPTOR = 2;
	private static final int DECOMPILE = 3;
	private static final int EXTRACT = 4;
	private static final int CONSTS = 5;
	private static final int PATCH = 6;
	private static final int COPYSRC = 7;
	private static final int RECOMPILE = 8;
	private static final int MD5 = 9;
	private static final int STEPS = 9;

	public TaskDecompile(int side, TaskInfo info) {
		super(side, info);
		decompiler = new Decompiler();
		md5Task = new TaskUpdateMD5(side, info);
		recompTask = new TaskRecompile(side, info);
		md5Task.recompile = false;
	}

	@Override
	public void doTask() throws Exception {
		String tinyOut 		= chooseFromSide(MCPConfig.CLIENT_TINY_OUT, MCPConfig.SERVER_TINY_OUT);
		String excOut 		= chooseFromSide(MCPConfig.CLIENT_EXC_OUT, MCPConfig.SERVER_EXC_OUT);
		String exc 			= chooseFromSide(MCPConfig.EXC_CLIENT, MCPConfig.EXC_SERVER);
		String srcZip 		= chooseFromSide(MCPConfig.CLIENT_SRC, MCPConfig.SERVER_SRC);
		Path originalJar 	= Paths.get(chooseFromSide(MCPConfig.CLIENT, MCPConfig.SERVER));
		Path ffOut 			= Paths.get(chooseFromSide(MCPConfig.CLIENT_TEMP_SOURCES, MCPConfig.SERVER_TEMP_SOURCES));
		Path srcPath 		= Paths.get(chooseFromSide(MCPConfig.CLIENT_SOURCES, MCPConfig.SERVER_SOURCES));
		Path patchesPath 	= Paths.get(chooseFromSide(MCPConfig.CLIENT_PATCHES, MCPConfig.SERVER_PATCHES));
		Path mappings		= Paths.get(chooseFromSide(MCPConfig.CLIENT_MAPPINGS, MCPConfig.SERVER_MAPPINGS));
		
		boolean hasLWJGL = side == CLIENT;
		
		if (Files.exists(srcPath)) {
			throw new IOException(chooseFromSide("Client", "Server") + " sources found! Aborting.");
		}
		for (Path path : new Path[] { Paths.get(tinyOut), Paths.get(excOut), Paths.get(srcZip)}) {
			Files.deleteIfExists(path);
		}
		FileUtil.createDirectories(Paths.get(MCPConfig.TEMP));
		FileUtil.deleteDirectoryIfExists(ffOut);
		while(step < STEPS) {
			step();
			switch (step) {
			case REMAP:
				if (Files.exists(mappings)) {
					Remapper.remap(mappings, originalJar, Paths.get(tinyOut), true, getLibraryPaths(side));
				}
				else {
					Files.copy(originalJar, Paths.get(tinyOut));
				}
				break;
			case EXCEPTOR:
				if (Files.exists(Paths.get(exc))) {
					MCInjector.process(tinyOut, excOut, exc, 0);
				}
				else {
					Files.copy(Paths.get(tinyOut), Paths.get(excOut));
				}
				// Copying a fixed jar to libs
				if(side == CLIENT) {
					Files.deleteIfExists(Paths.get(MCPConfig.CLIENT_FIXED));
					Files.copy(Paths.get(excOut), Paths.get(MCPConfig.CLIENT_FIXED));
				}
				break;
			case DECOMPILE:
				this.decompiler.decompile(excOut, srcZip, chooseFromSide(MCPConfig.JAVADOC_CLIENT, MCPConfig.JAVADOC_SERVER));
				break;
			case EXTRACT:
				FileUtil.createDirectories(Paths.get(MCPConfig.SRC));
				FileUtil.unzipByExtension(Paths.get(srcZip), ffOut, ".java");
				break;
			case CONSTS:
				if(hasLWJGL) {
					new GLConstants().replace(ffOut);
				}
				new MathConstants().replace(ffOut);
				break;
			case PATCH:
				if(MCP.config.patch && Files.exists(patchesPath)) {
					patch(ffOut, ffOut, patchesPath, info);
				}
				break;
			case COPYSRC:
				FileUtil.copyDirectory(ffOut, srcPath, MCP.config.ignorePackages);
				break;
			case RECOMPILE:
				recompTask.doTask();
				break;
			case MD5:
				md5Task.doTask();
				break;
			}
		}
	}
	
	private static void patch(Path base, Path out, Path patches, TaskInfo info) throws IOException {
		ByteArrayOutputStream logger = new ByteArrayOutputStream();
		PatchOperation patchOperation = PatchOperation.builder()
				.verbose(true)
				.basePath(base)
				.patchesPath(patches)
				.outputPath(out)
				.logTo(logger)
				.build();
		CliOperation.Result<PatchOperation.PatchesSummary> result = patchOperation.operate();
		if (result.exit != 0) {
			info.addInfo(logger.toString());
			info.addInfo("Patching failed!");
			throw new IOException("Could not apply patches!");
		}
	}

	public static Path[] getLibraryPaths(int side) {
		if(side == CLIENT) {
			return new Path[] {
				Paths.get(MCPConfig.LWJGL),
				Paths.get(MCPConfig.LWJGL_UTIL),
				Paths.get(MCPConfig.JINPUT)
			};
		}
		else {
			return new Path[] {};
		}
	}

	public ProgressInfo getProgress() {
		int total = 100;
		int current = 0;
		switch (step) {
		case REMAP:
			current = 1;
			return new ProgressInfo("Remapping JAR...", current, total);
		case EXCEPTOR:
			current = 2;
			return new ProgressInfo("Applying MCInjector...", current, total);
		case DECOMPILE: {
			current = 3;
			ProgressInfo info = decompiler.log.initInfo();
			int percent = (int)((double)info.getCurrent() / info.getTotal() * 80);
			return new ProgressInfo(info.getMessage(), current + percent, total); }
		case EXTRACT:
			current = 84;
			return new ProgressInfo("Extracting sources...", current, total);
		case PATCH:
			current = 85;
			return new ProgressInfo("Applying patches...", current, total);
		case CONSTS:
			current = 86;
			return new ProgressInfo("Replacing constants...", current, total);
		case COPYSRC:
			current = 87;
			return new ProgressInfo("Copying sources...", current, total);
		case RECOMPILE: {
			current = 88;
			ProgressInfo info = recompTask.getProgress();
			int percent = (int)((double)info.getCurrent() / info.getTotal() * 2);
			return new ProgressInfo(info.getMessage(), current + percent, total); }
		case MD5: {
			current = 91;
			ProgressInfo info = md5Task.getProgress();
			int percent = (int)((double)info.getCurrent() / info.getTotal() * 9);
			return new ProgressInfo(info.getMessage(), current + percent, total); }
		default:
			return super.getProgress();
		}
	}
}
