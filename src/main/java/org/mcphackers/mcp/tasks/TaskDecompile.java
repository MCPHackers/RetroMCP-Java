package org.mcphackers.mcp.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.ProgressListener;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.mcp.tools.mcinjector.MCInjector;
import org.mcphackers.mcp.tools.source.Constants;
import org.mcphackers.mcp.tools.source.GLConstants;
import org.mcphackers.mcp.tools.source.MathConstants;

import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class TaskDecompile extends Task {
	private final MemoryMappingTree mappingTree = new MemoryMappingTree();
	
	private static final int REMAP = 1;
	private static final int EXCEPTOR = 2;
	private static final int DECOMPILE = 3;
	private static final int EXTRACT = 4;
	private static final int CONSTS = 5;
	private static final int PATCH = 6;
	private static final int COPYSRC = 7;
	private static final int RECOMPILE = 8;
	private static final int MD5 = 9;
	private static final int STEPS = MD5;

	public TaskDecompile(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskDecompile(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	@Override
	public void doTask() throws Exception {
		Path tinyOut 		= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT_TINY_OUT, MCPPaths.SERVER_TINY_OUT));
		Path excOut 		= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT_EXC_OUT, MCPPaths.SERVER_EXC_OUT));
		Path exc 			= MCPPaths.get(mcp, chooseFromSide(MCPPaths.EXC_CLIENT, MCPPaths.EXC_SERVER));
		Path srcZip 		= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT_SRC, MCPPaths.SERVER_SRC));
		Path originalJar 	= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT, MCPPaths.SERVER));
		Path ffOut 			= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT_TEMP_SOURCES, MCPPaths.SERVER_TEMP_SOURCES));
		Path srcPath 		= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT_SOURCES, MCPPaths.SERVER_SOURCES));
		Path patchesPath 	= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT_PATCHES, MCPPaths.SERVER_PATCHES));
		Path mappings		= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT_MAPPINGS, MCPPaths.SERVER_MAPPINGS));
		Path deobfMappings	= MCPPaths.get(mcp, chooseFromSide(MCPPaths.CLIENT_MAPPINGS_DO, MCPPaths.SERVER_MAPPINGS_DO));
		Path javadocs		= MCPPaths.get(mcp, chooseFromSide(MCPPaths.JAVADOC_CLIENT, MCPPaths.JAVADOC_SERVER));
		
		boolean hasLWJGL = side == Side.CLIENT;
		
		if (Files.exists(srcPath)) {
			throw new IOException(chooseFromSide("Client", "Server") + " sources found! Aborting.");
		}
		for (Path path : new Path[] { tinyOut, excOut, srcZip}) {
			Files.deleteIfExists(path);
		}
		FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.TEMP));
		FileUtil.deleteDirectoryIfExists(ffOut);
		while(step < STEPS) {
			step();
			switch (step) {
			case REMAP:
				if (Files.exists(mappings)) {
					MappingUtil.readMappings(mappings, mappingTree);
					MappingUtil.modifyClasses(mappingTree, FileSystems.newFileSystem(originalJar, null).getPath("/"), className -> {
						if (mappingTree.getClass(className) == null) {
							if(className.lastIndexOf("/") < 0) {
								return "net/minecraft/src/" + className;
							}
						}
						return null;
					});
					MappingUtil.writeMappings(deobfMappings, mappingTree);
					MappingUtil.remap(deobfMappings, originalJar, tinyOut, getLibraryPaths(mcp, side), "official", "named");
				}
				else {
					Files.copy(originalJar, tinyOut);
				}
				break;
			case EXCEPTOR:
				if (Files.exists(exc)) {
					MCInjector.process(tinyOut, excOut, exc, 0);
				}
				else {
					Files.copy(tinyOut, excOut);
				}
				// Copying a fixed jar to libs
				if(side == Side.CLIENT) {
					Files.deleteIfExists(MCPPaths.get(mcp, MCPPaths.CLIENT_FIXED));
					Files.copy(excOut, MCPPaths.get(mcp, MCPPaths.CLIENT_FIXED));
				}
				break;
			case DECOMPILE:
				 new Decompiler(this).decompile(excOut, srcZip, javadocs, mcp.getOptions().getStringParameter(TaskParameter.INDENTION_STRING));
				break;
			case EXTRACT:
				FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.SRC));
				FileUtil.unzipByExtension(srcZip, ffOut, ".java");
				break;
			case CONSTS:
				List<Constants> constants = new ArrayList<>();
				if(hasLWJGL)
				constants.add(new GLConstants());
				constants.add(new MathConstants());
				Constants.replace(ffOut, constants);
				break;
			case PATCH:
				if(mcp.getOptions().getBooleanParameter(TaskParameter.PATCHES) && Files.exists(patchesPath)) {
					patch(ffOut, ffOut, patchesPath);
				}
				break;
			case COPYSRC:
				FileUtil.deletePackages(ffOut, mcp.getOptions().getStringArrayParameter(TaskParameter.IGNORED_PACKAGES));
				FileUtil.copyDirectory(ffOut, srcPath);
				break;
			case RECOMPILE:
				new TaskRecompile(side, mcp, this).doTask();
				break;
			case MD5:
				TaskUpdateMD5 md5Task = new TaskUpdateMD5(side, mcp, this);
				md5Task.recompile = false;
				md5Task.doTask();
				break;
			}
		}
	}

	private void patch(Path base, Path out, Path patches) throws IOException {
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
			addMessage(logger.toString(), Task.INFO);
			throw new IOException("Patching failed!");
		}
	}

	public static Path[] getLibraryPaths(MCP mcp, Side side) {
		if(side == Side.CLIENT) {
			return new Path[] {
				MCPPaths.get(mcp, MCPPaths.LWJGL),
				MCPPaths.get(mcp, MCPPaths.LWJGL_UTIL),
				MCPPaths.get(mcp, MCPPaths.JINPUT)
			};
		}
		else {
			return new Path[] {};
		}
	}
	
	public void setProgress(int progress) {
		switch (step) {
		case DECOMPILE: {
			int percent = (int)((double)progress * 0.8D);
			super.setProgress(3 + percent);
			break;
		}
		case RECOMPILE: {
			int percent = (int)((double)progress * 0.05D);
			super.setProgress(91 + percent);
			break;
		}
		case MD5: {
			int percent = (int)((double)progress * 0.04D);
			super.setProgress(96 + percent);
			break;
		}
		default:
			super.setProgress(progress);
			break;
		}
	}

	protected void updateProgress() {
		switch (step) {
		case REMAP:
			setProgress("Remapping JAR...", 1);
			break;
		case EXCEPTOR:
			setProgress("Applying MCInjector...", 2);
			break;
		case DECOMPILE:
			setProgress("Decompiling...");
			break;
		case EXTRACT:
			setProgress("Extracting sources...", 84);
			break;
		case PATCH:
			setProgress("Applying patches...", 86);
			break;
		case CONSTS:
			setProgress("Replacing constants...", 88);
			break;
		case COPYSRC:
			setProgress("Copying sources...", 90);
			break;
		case RECOMPILE:
			setProgress("Recompiling...");
			break;
		case MD5:
			break;
		default:
			super.updateProgress();
			break;
		}
	}
}
