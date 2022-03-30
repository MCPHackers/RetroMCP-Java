package org.mcphackers.mcp.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.ProgressListener;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.constants.Constants;
import org.mcphackers.mcp.tools.constants.GLConstants;
import org.mcphackers.mcp.tools.constants.MathConstants;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.mcp.tools.mcinjector.MCInjector;

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
		String tinyOut 		= chooseFromSide(MCPPaths.CLIENT_TINY_OUT, MCPPaths.SERVER_TINY_OUT);
		String excOut 		= chooseFromSide(MCPPaths.CLIENT_EXC_OUT, MCPPaths.SERVER_EXC_OUT);
		String exc 			= chooseFromSide(MCPPaths.EXC_CLIENT, MCPPaths.EXC_SERVER);
		String srcZip 		= chooseFromSide(MCPPaths.CLIENT_SRC, MCPPaths.SERVER_SRC);
		Path originalJar 	= Paths.get(chooseFromSide(MCPPaths.CLIENT, MCPPaths.SERVER));
		Path ffOut 			= Paths.get(chooseFromSide(MCPPaths.CLIENT_TEMP_SOURCES, MCPPaths.SERVER_TEMP_SOURCES));
		Path srcPath 		= Paths.get(chooseFromSide(MCPPaths.CLIENT_SOURCES, MCPPaths.SERVER_SOURCES));
		Path patchesPath 	= Paths.get(chooseFromSide(MCPPaths.CLIENT_PATCHES, MCPPaths.SERVER_PATCHES));
		Path mappings		= Paths.get(chooseFromSide(MCPPaths.CLIENT_MAPPINGS, MCPPaths.SERVER_MAPPINGS));
		Path deobfMappings	= Paths.get(chooseFromSide(MCPPaths.CLIENT_MAPPINGS_DO, MCPPaths.SERVER_MAPPINGS_DO));
		
		boolean hasLWJGL = side == Side.CLIENT;
		
		if (Files.exists(srcPath)) {
			throw new IOException(chooseFromSide("Client", "Server") + " sources found! Aborting.");
		}
		for (Path path : new Path[] { Paths.get(tinyOut), Paths.get(excOut), Paths.get(srcZip)}) {
			Files.deleteIfExists(path);
		}
		FileUtil.createDirectories(Paths.get(MCPPaths.TEMP));
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
					MappingUtil.remap(deobfMappings, originalJar, Paths.get(tinyOut), getLibraryPaths(side), "official", "named");
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
				if(side == Side.CLIENT) {
					Files.deleteIfExists(Paths.get(MCPPaths.CLIENT_FIXED));
					Files.copy(Paths.get(excOut), Paths.get(MCPPaths.CLIENT_FIXED));
				}
				break;
			case DECOMPILE:
				 new Decompiler(this).decompile(excOut, srcZip, chooseFromSide(MCPPaths.JAVADOC_CLIENT, MCPPaths.JAVADOC_SERVER), mcp.getStringParam(TaskParameter.INDENTION_STRING));
				break;
			case EXTRACT:
				FileUtil.createDirectories(Paths.get(MCPPaths.SRC));
				FileUtil.unzipByExtension(Paths.get(srcZip), ffOut, ".java");
				break;
			case CONSTS:
				List<Constants> constants = new ArrayList<>();
				if(hasLWJGL)
				constants.add(new GLConstants());
				constants.add(new MathConstants());
				Constants.replace(ffOut, constants);
				break;
			case PATCH:
				if(mcp.getBooleanParam(TaskParameter.PATCHES) && Files.exists(patchesPath)) {
					patch(ffOut, ffOut, patchesPath);
				}
				break;
			case COPYSRC:
				FileUtil.deletePackages(ffOut, mcp.getStringArrayParam(TaskParameter.IGNORED_PACKAGES));
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

	@Override
	public String getName() {
		return "Decompile";
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
			// TODO
			//info.addInfo(logger.toString());
			throw new IOException("Patching failed!");
		}
	}

	public static Path[] getLibraryPaths(Side side) {
		if(side == Side.CLIENT) {
			return new Path[] {
				Paths.get(MCPPaths.LWJGL),
				Paths.get(MCPPaths.LWJGL_UTIL),
				Paths.get(MCPPaths.JINPUT)
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
