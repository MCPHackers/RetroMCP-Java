package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.ClassUtils;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.injector.GLConstants;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.mcp.tools.project.EclipseProjectWriter;
import org.mcphackers.mcp.tools.project.IdeaProjectWriter;
import org.mcphackers.mcp.tools.project.VSCProjectWriter;
import org.mcphackers.mcp.tools.source.Source;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.injector.transform.Transform;
import org.mcphackers.rdi.nio.IOUtil;
import org.mcphackers.rdi.nio.MappingsIO;
import org.mcphackers.rdi.nio.RDInjector;
import org.objectweb.asm.tree.ClassNode;

public class TaskDecompile extends TaskStaged {
	public static final int STAGE_DECOMPILE = 2;
	public static final int STAGE_MD5 = 5;

	private int classVersion = -1;

	public TaskDecompile(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskDecompile(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	public static Mappings getMappings(Path mappingsPath, ClassStorage storage, Side side) throws IOException {
		if (!Files.exists(mappingsPath)) {
			return null;
		}
		boolean joined = MappingUtil.readNamespaces(mappingsPath).contains("official");
		Mappings mappings = MappingsIO.read(mappingsPath, joined ? "official" : side.name, "named");
		for (String name : storage.getAllClasses()) {
			if (name.indexOf('/') == -1 && !mappings.classes.containsKey(name)) {
				mappings.classes.put(name, "net/minecraft/src/" + name);
			}
		}
		return mappings;
	}

	@Override
	protected Stage[] setStages() {
		final Path rdiOut = MCPPaths.get(mcp, REMAPPED, side);
		final Path ffOut = MCPPaths.get(mcp, SOURCE_UNPATCHED, side);
		final Path srcPath = MCPPaths.get(mcp, SOURCE, side);
		final Path patchesPath = MCPPaths.get(mcp, CONF_PATCHES, side);

		return new Stage[]{stage(getLocalizedStage("prepare"), 0, () -> {
			FileUtil.cleanDirectory(MCPPaths.get(mcp, PROJECT, side));
			FileUtil.createDirectories(MCPPaths.get(mcp, JARS_DIR, side));
			FileUtil.createDirectories(MCPPaths.get(mcp, MD5_DIR, side));
			Files.createDirectories(MCPPaths.get(mcp, GAMEDIR, side));
		}), stage(getLocalizedStage("rdi"), 2, () -> {
			ClassStorage storage = applyInjector();
			for (ClassNode node : storage) {
				classVersion = Math.max(classVersion, node.version);
			}
			// Force Java 8 or later in order to support VSC
			// Java extension does not allow compiling under Java 8
			classVersion = Math.max(52, classVersion);
		}), stage(getLocalizedStage("decompile"), 0, () -> {
			new Decompiler(this, rdiOut, ffOut, mcp.getLibraries(), mcp).decompile();
			new EclipseProjectWriter().createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
			new IdeaProjectWriter().createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
			new VSCProjectWriter().createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
		}), stage(getLocalizedStage("patch"), 88, () -> {
			if (mcp.getOptions().getBooleanParameter(TaskParameter.PATCHES) && Files.exists(patchesPath)) {
				TaskApplyPatch.patch(this, ffOut, ffOut, patchesPath);
			}

			Source.modify(ffOut, MCP.SOURCE_ADAPTERS);
		}), stage(getLocalizedStage("copysrc"), 90, () -> {
			if (!mcp.getOptions().getBooleanParameter(TaskParameter.DECOMPILE_RESOURCES)) {
				for (Path p : FileUtil.walkDirectory(ffOut, p -> !Files.isDirectory(p) && !p.getFileName().toString().endsWith(".java"))) {
					Files.delete(p);
				}
			}
			Files.createDirectories(srcPath);
			FileUtil.compress(ffOut, MCPPaths.get(mcp, SOURCE_JAR, side));
			if (mcp.getOptions().getBooleanParameter(TaskParameter.OUTPUT_SRC)) {
				FileUtil.deletePackages(ffOut, mcp.getOptions().getStringArrayParameter(TaskParameter.IGNORED_PACKAGES));
				FileUtil.copyDirectory(ffOut, srcPath);
			}
		}), stage(getLocalizedStage("recompile"), 95, () -> new TaskUpdateMD5(side, mcp, this).doTask()),};
	}

	public ClassStorage applyInjector() throws IOException {
		final Path rdiOut = MCPPaths.get(mcp, REMAPPED, side);
		final Path mappingsPath = MCPPaths.get(mcp, MAPPINGS);
		final boolean guessGenerics = mcp.getOptions().getBooleanParameter(TaskParameter.GUESS_GENERICS);
		final boolean stripGenerics = mcp.getOptions().getBooleanParameter(TaskParameter.STRIP_GENERICS);
		final boolean hasLWJGL = side == Side.CLIENT || side == Side.MERGED;

		RDInjector injector = new RDInjector();
		Path path;
		Mappings mappings;

		if (side == Side.MERGED) {
			path = MCPPaths.get(mcp, JAR_ORIGINAL, Side.SERVER);
			injector.setStorage(new ClassStorage(IOUtil.readJar(path)));
			injector.addResources(path);
			if (stripGenerics) {
				injector.stripLVT();
				injector.addTransform(Transform::stripSignatures);
			}
			mappings = getMappings(mappingsPath, injector.getStorage(), Side.SERVER);
			if (mappings != null) {
				injector.applyMappings(mappings);
			}
			injector.transform();
			ClassStorage serverStorage = injector.getStorage();

			path = MCPPaths.get(mcp, JAR_ORIGINAL, Side.CLIENT);
			injector.setStorage(new ClassStorage(IOUtil.readJar(path)));
			injector.addResources(path);
			if (stripGenerics) {
				injector.stripLVT();
				injector.addTransform(Transform::stripSignatures);
			}
			mappings = getMappings(mappingsPath, injector.getStorage(), Side.CLIENT);
			if (mappings != null) {
				injector.applyMappings(mappings);
			}
			injector.mergeWith(serverStorage);
		} else {
			path = MCPPaths.get(mcp, JAR_ORIGINAL, side);
			injector.setStorage(new ClassStorage(IOUtil.readJar(path)));
			injector.addResources(path);
			if (stripGenerics) {
				injector.stripLVT();
				injector.addTransform(Transform::stripSignatures);
			}
			mappings = getMappings(mappingsPath, injector.getStorage(), side);
			if (mappings != null) {
				injector.applyMappings(mappings);
			}
		}
		injector.addTransform(Transform::decomposeVars);
		injector.addTransform(Transform::replaceCommonConstants);
		if (hasLWJGL) injector.addVisitor(new GLConstants(null));
		injector.restoreSourceFile();
		injector.fixInnerClasses();
		injector.fixImplicitConstructors();
		if (guessGenerics) injector.guessGenerics();
		final Path exc = MCPPaths.get(mcp, EXC);
		if (Files.exists(exc)) {
			injector.fixExceptions(exc);
		}
		if (side == Side.MERGED) {
			Path acc = MCPPaths.get(mcp, MCPPaths.ACCESS, Side.CLIENT);
			if (Files.exists(acc)) {
				injector.fixAccess(acc);
			}
			acc = MCPPaths.get(mcp, MCPPaths.ACCESS, Side.SERVER);
			if (Files.exists(acc)) {
				injector.fixAccess(acc);
			}
		} else {
			final Path acc = MCPPaths.get(mcp, MCPPaths.ACCESS, side);
			if (Files.exists(acc)) {
				injector.fixAccess(acc);
			}
		}
		injector.transform();
		injector.write(rdiOut);
		return injector.getStorage();
	}

	@Override
	public void setProgress(int progress) {
		switch (step) {
			case STAGE_DECOMPILE: {
				int percent = (int) (progress * 0.82D);
				super.setProgress(3 + percent);
				break;
			}
			case STAGE_MD5: {
				int percent = (int) (progress * 0.04D);
				super.setProgress(96 + percent);
				break;
			}
			default:
				super.setProgress(progress);
				break;
		}
	}
}
