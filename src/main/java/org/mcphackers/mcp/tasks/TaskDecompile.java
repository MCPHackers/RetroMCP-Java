package org.mcphackers.mcp.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.mappings.MappingData;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.mcp.tools.source.Constants;
import org.mcphackers.mcp.tools.source.GLConstants;
import org.mcphackers.mcp.tools.source.MathConstants;
import org.mcphackers.rdi.injector.RDInjector;
import org.mcphackers.rdi.util.IOUtil;

import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;
import net.fabricmc.stitch.merge.JarMerger;

public class TaskDecompile extends TaskStaged {
	/*
	 * Indexes of stages for plugin overrides
	 */
	public static final int STAGE_INIT = 0;
	public static final int STAGE_REMAP = 1;
	public static final int STAGE_EXCEPTOR = 2;
	public static final int STAGE_DECOMPILE = 3;
	public static final int STAGE_EXTRACT = 4;
	public static final int STAGE_CONSTS = 5;
	public static final int STAGE_PATCH = 6;
	public static final int STAGE_COPYSRC = 7;
	public static final int STAGE_MD5 = 8;

	public TaskDecompile(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskDecompile(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	@Override
	protected Stage[] setStages() {
		final Path remapped 	= MCPPaths.get(mcp, MCPPaths.DEOBF_OUT, side);
		final Path excOut 		= MCPPaths.get(mcp, MCPPaths.EXC_OUT, side);
		final Path tempExcOut 	= MCPPaths.get(mcp, MCPPaths.TEMP_EXC_OUT, side);
		final Path ffOut 		= MCPPaths.get(mcp, MCPPaths.TEMP_SOURCES, side);
		final Path srcZip 		= MCPPaths.get(mcp, MCPPaths.SIDE_SRC, side);
		final Path srcPath 		= MCPPaths.get(mcp, MCPPaths.SOURCES, side);
		final Path patchesPath 	= MCPPaths.get(mcp, MCPPaths.PATCHES, side);
		
		boolean hasLWJGL = side == Side.CLIENT || side == Side.MERGED;
		
		return new Stage[] {
			stage("Preparing", 0,
			() -> {
				FileUtil.deleteDirectoryIfExists(srcPath);
				for (Path path : new Path[] {remapped, excOut, srcZip}) {
					Files.deleteIfExists(path);
				}
				FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.TEMP_SIDE, side));
				FileUtil.deleteDirectoryIfExists(ffOut);
			}),
			stage("Remapping JAR", 1,
			() -> {
				Side[] sides = (side == Side.MERGED) ? new Side[] {Side.CLIENT, Side.SERVER} : new Side[] {side};
				for(Side sideLocal : sides) {
					final Path mappings = MCPPaths.get(mcp, MCPPaths.MAPPINGS, sideLocal);
					final Path deobfMappings	= MCPPaths.get(mcp, MCPPaths.MAPPINGS_DO, sideLocal);
					final Path tinyOut 		= MCPPaths.get(mcp, MCPPaths.DEOBF_OUT, sideLocal);
					final Path originalJar 	= MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, sideLocal);
					FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.TEMP_SIDE, sideLocal));
					Files.deleteIfExists(tinyOut);
					if (Files.exists(mappings)) {
						MappingData mappingData = new MappingData(sideLocal, mappings);
						try (FileSystem fs = FileSystems.newFileSystem(originalJar, (ClassLoader)null)) {
							MappingUtil.modifyClasses(mappingData.getTree(), fs.getPath("/"), className -> {
								if (mappingData.getTree().getClass(className) == null) {
									if(className.lastIndexOf("/") < 0) {
										return "net/minecraft/src/" + className;
									}
								}
								return null;
							});
						}
						mappingData.save(deobfMappings);
						MappingUtil.remap(deobfMappings, originalJar, tinyOut, getLibraryPaths(mcp, sideLocal), "official", "named");
					}
					else {
						Files.copy(originalJar, tinyOut);
					}
				}
				if(side == Side.MERGED) {
					final Path client = MCPPaths.get(mcp, MCPPaths.DEOBF_OUT, Side.CLIENT);
					final Path server = MCPPaths.get(mcp, MCPPaths.DEOBF_OUT, Side.SERVER);
	
					try (JarMerger jarMerger = new JarMerger(client.toFile(), server.toFile(), remapped.toFile())) {
						jarMerger.enableSyntheticParamsOffset();
						jarMerger.merge();
					}
				}
			}),
			stage("Applying RDInjector", 2,
			() -> {
				Side[] sides = (side == Side.MERGED) ? new Side[] {Side.CLIENT, Side.SERVER} : new Side[] {side};
				Files.deleteIfExists(tempExcOut);
				Files.copy(remapped, tempExcOut);
				for(Side sideLocal : sides) {
					final Path exc = MCPPaths.get(mcp, MCPPaths.EXC, sideLocal);
					RDInjector injector = new RDInjector(tempExcOut);
					injector.fixInnerClasses();
					injector.fixSwitchMaps();
					injector.fixImplicitConstructors();
					injector.fixBridges();
					if (Files.exists(exc)) {
						injector.fixExceptions(exc);
					}
					injector.transform();
					IOUtil.write(injector, Files.newOutputStream(excOut), tempExcOut);
					Files.deleteIfExists(tempExcOut);
					Files.copy(excOut, tempExcOut);
				}
				// Copying a fixed jar to libs
				if(side == Side.CLIENT || side == Side.MERGED) {
					Files.deleteIfExists(MCPPaths.get(mcp, MCPPaths.CLIENT_FIXED));
					Files.copy(excOut, MCPPaths.get(mcp, MCPPaths.CLIENT_FIXED));
				}
				Files.deleteIfExists(tempExcOut);
			}),
			stage("Decompiling",
			() -> {
				//TODO Apply both javadocs if side == Side.MERGED
				//FIXME Javadocs
				final Path deobfMappings = MCPPaths.get(mcp, MCPPaths.MAPPINGS_DO, side == Side.MERGED ? Side.CLIENT : side);
				new Decompiler(this, excOut, srcZip, deobfMappings,
						mcp.getOptions().getStringParameter(TaskParameter.INDENTATION_STRING),
						mcp.getOptions().getBooleanParameter(TaskParameter.DECOMPILE_OVERRIDE))
				.decompile();
			}),
			stage("Extracting sources", 84,
			() -> {
					FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.SRC));
					FileUtil.unzipByExtension(srcZip, ffOut, ".java");
			}),
			stage("Replacing constants", 86,
			() -> {
					List<Constants> constants = new ArrayList<>();
					if(hasLWJGL)
						constants.add(new GLConstants());
					constants.add(new MathConstants());
					Constants.replace(ffOut, constants);
			}),
			stage("Applying patches", 88,
			() -> {
				if(mcp.getOptions().getBooleanParameter(TaskParameter.PATCHES) && Files.exists(patchesPath)) {
					patch(ffOut, ffOut, patchesPath);
				}
			}),
			stage("Copying sources", 90,
			() -> {
				FileUtil.deletePackages(ffOut, mcp.getOptions().getStringArrayParameter(TaskParameter.IGNORED_PACKAGES));
				FileUtil.copyDirectory(ffOut, srcPath);
			}),
			stage("Recompiling",
			() -> {
				new TaskUpdateMD5(side, mcp, this).doTask();
			}),
		};
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
		if(side == Side.CLIENT || side == Side.MERGED) {
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
		case STAGE_DECOMPILE: {
			int percent = (int)((double)progress * 0.8D);
			super.setProgress(3 + percent);
			break;
		}
		case STAGE_MD5: {
			int percent = (int)((double)progress * 0.04D);
			super.setProgress(96 + percent);
			break;
		}
		default:
			super.setProgress(progress);
			break;
		}
	}
}
