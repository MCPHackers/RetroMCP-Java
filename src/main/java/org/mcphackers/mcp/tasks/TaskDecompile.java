package org.mcphackers.mcp.tasks;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.injector.GLConstants;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.mcp.tools.source.MathConstants;
import org.mcphackers.mcp.tools.source.Source;
import org.mcphackers.mcp.tools.versions.DownloadData;
import org.mcphackers.rdi.injector.RDInjector;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.util.IOUtil;

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
		final Path ffOut 		= MCPPaths.get(mcp, MCPPaths.SOURCE_UNPATCHED, side);
		final Path srcPath 		= MCPPaths.get(mcp, MCPPaths.SOURCE, side);
		final Path patchesPath 	= MCPPaths.get(mcp, MCPPaths.PATCHES, side);
		
		final boolean guessGenerics = mcp.getOptions().getBooleanParameter(TaskParameter.GUESS_GENERICS);
		final boolean hasLWJGL = side == Side.CLIENT || side == Side.MERGED;
		
		return new Stage[] {
			stage(getLocalizedStage("prepare"), 0,
			() -> {
				FileUtil.cleanDirectory(MCPPaths.get(mcp, MCPPaths.PROJECT, side));
				FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.JARS_DIR, side));
				FileUtil.createDirectories(MCPPaths.get(mcp, MCPPaths.MD5_DIR, side));
			}),
			stage(getLocalizedStage("rdi"), 2,
			() -> {
				RDInjector injector = new RDInjector();
				Path path;
				Mappings mappings;
				
				if(side == Side.MERGED) {
					path = MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.SERVER);
					injector.setStorage(new ClassStorage(IOUtil.read(path)));
					injector.addResources(path);
					injector.stripLVT();
					mappings = getMappings(injector.getStorage(), Side.SERVER);
					if(mappings != null) {
						injector.applyMappings(mappings);
					}
					injector.transform();
					ClassStorage serverStorage = injector.getStorage();
					
					path = MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, Side.CLIENT);
					injector.setStorage(new ClassStorage(IOUtil.read(path)));
					injector.addResources(path);
					injector.stripLVT();
					mappings = getMappings(injector.getStorage(), Side.CLIENT);
					if(mappings != null) {
						injector.applyMappings(mappings);
					}
					injector.mergeWith(serverStorage);
				}
				else {
					path = MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side);
					injector.setStorage(new ClassStorage(IOUtil.read(path)));
					injector.addResources(path);
					injector.stripLVT();
					mappings = getMappings(injector.getStorage(), side);
					if(mappings != null) {
						injector.applyMappings(mappings);
					}
				}
				if(hasLWJGL) injector.addVisitor(new GLConstants(null));
				injector.restoreSourceFile();
				injector.fixInnerClasses();
				injector.fixImplicitConstructors();
				if(guessGenerics) {
					injector.guessGenerics();
				}
				final Path exc = MCPPaths.get(mcp, MCPPaths.EXC);
				if (Files.exists(exc)) {
					injector.fixExceptions(exc);
				}
				injector.transform();
				injector.write(rdiOut);
			}),
			stage(getLocalizedStage("decompile"),
			() -> {
				new Decompiler(this, rdiOut, ffOut,
						mcp.getOptions().getStringParameter(TaskParameter.INDENTATION_STRING),
						guessGenerics)
				.decompile();
				createProject(side);
			}),
			stage(getLocalizedStage("constants"), 86,
			() -> {
				Source.modify(ffOut, Collections.singletonList(new MathConstants()));
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
	
	private Mappings getMappings(ClassStorage storage, Side side) throws IOException {
		Path mappingsPath = MCPPaths.get(mcp, MCPPaths.MAPPINGS);
		if(!Files.exists(mappingsPath)) {
			return null;
		}
		boolean joined = MappingUtil.readNamespaces(mappingsPath).contains("official");
		Mappings mappings = Mappings.read(mappingsPath, joined ? "official" : side.name, "named");
		for(String name : storage.getAllClasses()) {
			if(name.indexOf('/') == -1 && !mappings.classes.containsKey(name)) {
				mappings.classes.put(name, "net/minecraft/src/" + name);
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
	
	private void createProject(Side side) throws IOException {
		List<String> libraries = DownloadData.getLibraries(mcp.getCurrentVersion());
		
		try (BufferedWriter writer = Files.newBufferedWriter(MCPPaths.get(mcp, MCPPaths.PROJECT, side).resolve(".classpath"))) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); writer.newLine();
			writer.write("<classpath>"); writer.newLine();
			writer.write("\t<classpathentry kind=\"src\" path=\"src\">"); writer.newLine();
			writer.write("\t\t<attributes>"); writer.newLine();
			writer.write("\t\t\t<attribute name=\"org.eclipse.jdt.launching.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY\" value=\"../libraries/natives\"/>"); writer.newLine();
			writer.write("\t\t</attributes>"); writer.newLine();
			writer.write("\t</classpathentry>"); writer.newLine();
			writer.write("\t<classpathentry kind=\"lib\" path=\"jars/deobfuscated.jar\"/>"); writer.newLine();
			for(String lib : libraries) {
				if(Files.exists(MCPPaths.get(mcp, "libraries/" + lib + ".jar"))) {
					writer.write("\t<classpathentry kind=\"lib\" path=\"../libraries/" + lib + ".jar\"/>"); writer.newLine();
				}
			}
			writer.write("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"/>"); writer.newLine();
			writer.write("\t<classpathentry kind=\"output\" path=\"output\"/>"); writer.newLine();
			writer.write("</classpath>"); writer.newLine();
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
