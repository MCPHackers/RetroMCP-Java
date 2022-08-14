package org.mcphackers.mcp.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.source.Constants;
import org.mcphackers.mcp.tools.source.GLConstants;
import org.mcphackers.mcp.tools.source.MathConstants;
import org.mcphackers.rdi.injector.RDInjector;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.util.IOUtil;
import org.objectweb.asm.tree.ClassNode;

import codechicken.diffpatch.cli.CliOperation;
import codechicken.diffpatch.cli.PatchOperation;

public class TaskDecompile extends TaskStaged {
	/*
	 * Indexes of stages for plugin overrides
	 */
	public static final int STAGE_INIT = 0;
	public static final int STAGE_EXCEPTOR = 1;
	public static final int STAGE_DECOMPILE = 2;
	public static final int STAGE_CONSTS = 3;
	public static final int STAGE_PATCH = 4;
	public static final int STAGE_COPYSRC = 5;
	public static final int STAGE_MD5 = 6;

	public TaskDecompile(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskDecompile(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	@Override
	protected Stage[] setStages() {
		final Path rdiOut 		= MCPPaths.get(mcp, MCPPaths.REMAPPED, side);
		final Path ffOut 		= MCPPaths.get(mcp, MCPPaths.TEMP_SRC, side);
		final Path srcPath 		= MCPPaths.get(mcp, MCPPaths.SOURCE, side);
		final Path patchesPath 	= MCPPaths.get(mcp, MCPPaths.PATCHES, side);
		
		boolean hasLWJGL = side == Side.CLIENT || side == Side.MERGED;
		
		return new Stage[] {
			stage(getLocalizedStage("prepare"), 0,
			() -> {
				FileUtil.deleteDirectoryIfExists(srcPath);
				FileUtil.deleteDirectoryIfExists(ffOut);
				Files.deleteIfExists(rdiOut);
				FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.TEMP_SIDE, side));
			}),
			stage(getLocalizedStage("rdi"), 2,
			() -> {
				RDInjector injector = new RDInjector();
				if(side == Side.MERGED) {
					injector.setStorage(new ClassStorage(IOUtil.read(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER))));
					injector.stripLVT();
					injector.applyMappings(getMappings(injector.getStorage(), Side.SERVER));
					injector.transform();
					ClassStorage serverStorage = injector.getStorage();
					
					injector.setStorage(new ClassStorage(IOUtil.read(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT))));
					injector.stripLVT();
					injector.applyMappings(getMappings(injector.getStorage(), Side.CLIENT));
					injector.mergeWith(serverStorage);
				}
				else {
					injector.setStorage(new ClassStorage(IOUtil.read(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side))));
					injector.stripLVT();
					injector.applyMappings(getMappings(injector.getStorage(), side));
				}
				injector.fixAccess();
				injector.fixInnerClasses();
				injector.fixImplicitConstructors();
				//injector.guessGenerics();
				final Path exc = MCPPaths.get(mcp, MCPPaths.EXC);
				if (Files.exists(exc)) {
					injector.fixExceptions(exc);
				}
				injector.transform();
				//TODO Allow copying resources from multiple sources
				if(side == Side.MERGED) {
					IOUtil.write(injector.getStorage(), Files.newOutputStream(rdiOut), MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT));
				}
				else {
					IOUtil.write(injector.getStorage(), Files.newOutputStream(rdiOut), MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
				}
			}),
			stage(getLocalizedStage("decompile"),
			() -> {
				final Path javadocs = MCPPaths.get(mcp, MCPPaths.MAPPINGS);
				new Decompiler(this, rdiOut, ffOut, javadocs,
						mcp.getOptions().getStringParameter(TaskParameter.INDENTATION_STRING),
						mcp.getOptions().getBooleanParameter(TaskParameter.DECOMPILE_OVERRIDE))
				.decompile();
			}),
			stage(getLocalizedStage("constants"), 86,
			() -> {
				List<Constants> constants = new ArrayList<>();
				if(hasLWJGL)
				constants.add(new GLConstants());
				constants.add(new MathConstants());
				Constants.replace(ffOut, constants);
			}),
			stage(getLocalizedStage("patch"), 88,
			() -> {
				if(mcp.getOptions().getBooleanParameter(TaskParameter.PATCHES) && Files.exists(patchesPath)) {
					patch(ffOut, ffOut, patchesPath);
				}
			}),
			stage(getLocalizedStage("copysrc"), 90,
			() -> {
				if(!mcp.getOptions().getBooleanParameter(TaskParameter.DECOMPILE_RESOURCES)) {
					for(Path p : FileUtil.walkDirectory(ffOut, p -> !Files.isDirectory(p) && !p.getFileName().toString().endsWith(".java"))) {
						Files.delete(p);
					}
				}
				FileUtil.deletePackages(ffOut, mcp.getOptions().getStringArrayParameter(TaskParameter.IGNORED_PACKAGES));
				FileUtil.copyDirectory(ffOut, srcPath);
			}),
			stage(getLocalizedStage("recompile"),
			() -> {
				new TaskUpdateMD5(side, mcp, this).doTask();
			}),
		};
	}
	
	private Mappings getMappings(ClassStorage storage, Side side) {
		Mappings mappings = Mappings.read(MCPPaths.get(mcp, MCPPaths.MAPPINGS), side.name, "named");
		for(ClassNode node : storage.getClasses()) {
			if(node.name.indexOf('/') == -1 && !mappings.classes.containsKey(node.name)) {
				mappings.classes.put(node.name, "net/minecraft/src/" + node.name);
			}
		}
		return mappings;
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
			addMessage("Patching failed!", Task.ERROR);
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
