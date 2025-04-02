package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.ClassUtils;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.XMLWriter;
import org.mcphackers.mcp.tools.fernflower.Decompiler;
import org.mcphackers.mcp.tools.injector.GLConstants;
import org.mcphackers.mcp.tools.mappings.MappingUtil;
import org.mcphackers.mcp.tools.versions.json.DependDownload;
import org.mcphackers.mcp.tools.versions.json.Rule;
import org.mcphackers.mcp.tools.versions.json.Version;
import org.mcphackers.rdi.injector.data.ClassStorage;
import org.mcphackers.rdi.injector.data.Mappings;
import org.mcphackers.rdi.injector.transform.Transform;
import org.mcphackers.rdi.nio.MappingsIO;
import org.mcphackers.rdi.nio.RDInjector;
import org.mcphackers.rdi.nio.IOUtil;
import org.objectweb.asm.tree.ClassNode;

public class TaskDecompile extends TaskStaged {
	/*
	 * Indexes of stages for plugin overrides
	 */
	public static final int STAGE_INIT = 0;
	public static final int STAGE_EXCEPTOR = 1;
	public static final int STAGE_DECOMPILE = 2;
	public static final int STAGE_PATCH = 3;
	public static final int STAGE_COPYSRC = 4;
	public static final int STAGE_MD5 = 5;

	private int classVersion = -1;

	public TaskDecompile(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskDecompile(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

	@Override
	protected Stage[] setStages() {
		final Path rdiOut 		= MCPPaths.get(mcp, REMAPPED, side);
		final Path ffOut 		= MCPPaths.get(mcp, SOURCE_UNPATCHED, side);
		final Path srcPath 		= MCPPaths.get(mcp, SOURCE, side);
		final Path patchesPath 	= MCPPaths.get(mcp, PATCHES, side);

		final boolean guessGenerics = mcp.getOptions().getBooleanParameter(TaskParameter.GUESS_GENERICS);

		return new Stage[] {
			stage(getLocalizedStage("prepare"), 0,
			() -> {
				FileUtil.cleanDirectory(MCPPaths.get(mcp, PROJECT, side));
				FileUtil.createDirectories(MCPPaths.get(mcp, JARS_DIR, side));
				FileUtil.createDirectories(MCPPaths.get(mcp, MD5_DIR, side));
			}),
			stage(getLocalizedStage("rdi"), 2,
			() -> {
				ClassStorage storage = applyInjector();
				for(ClassNode node : storage) {
					classVersion = Math.max(classVersion, node.version);
				}
			}),
			stage(getLocalizedStage("decompile"),
			() -> {
				new Decompiler(this, rdiOut, ffOut, mcp.getLibraries(),
						mcp.getOptions().getStringParameter(TaskParameter.INDENTATION_STRING),
						guessGenerics)
				.decompile();
				createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
			}),
			stage(getLocalizedStage("patch"), 88,
			() -> {
				if(mcp.getOptions().getBooleanParameter(TaskParameter.PATCHES) && Files.exists(patchesPath)) {
					TaskApplyPatch.patch(this, ffOut, ffOut, patchesPath);
				}
			}),
			stage(getLocalizedStage("copysrc"), 90,
			() -> {
				if(!mcp.getOptions().getBooleanParameter(TaskParameter.DECOMPILE_RESOURCES)) {
					for(Path p : FileUtil.walkDirectory(ffOut, p -> !Files.isDirectory(p) && !p.getFileName().toString().endsWith(".java"))) {
						Files.delete(p);
					}
				}
				Files.createDirectories(srcPath);
				FileUtil.compress(ffOut, MCPPaths.get(mcp, SOURCE_JAR, side));
				if(mcp.getOptions().getBooleanParameter(TaskParameter.OUTPUT_SRC)) {
					FileUtil.deletePackages(ffOut, mcp.getOptions().getStringArrayParameter(TaskParameter.IGNORED_PACKAGES));
					FileUtil.copyDirectory(ffOut, srcPath);
				}
				Files.createDirectories(MCPPaths.get(mcp, GAMEDIR, side));
			}),
			stage(getLocalizedStage("recompile"),
			() -> new TaskUpdateMD5(side, mcp, this).doTask()),
		};
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

		if(side == Side.MERGED) {
			path = MCPPaths.get(mcp, JAR_ORIGINAL, Side.SERVER);
			injector.setStorage(new ClassStorage(IOUtil.readJar(path)));
			injector.addResources(path);
			if(stripGenerics) {
				injector.stripLVT();
				injector.addTransform(Transform::stripSignatures);
			}
			mappings = getMappings(mappingsPath, injector.getStorage(), Side.SERVER);
			if(mappings != null) {
				injector.applyMappings(mappings);
			}
			injector.transform();
			ClassStorage serverStorage = injector.getStorage();

			path = MCPPaths.get(mcp, JAR_ORIGINAL, Side.CLIENT);
			injector.setStorage(new ClassStorage(IOUtil.readJar(path)));
			injector.addResources(path);
			if(stripGenerics) {
				injector.stripLVT();
				injector.addTransform(Transform::stripSignatures);
			}
			mappings = getMappings(mappingsPath, injector.getStorage(), Side.CLIENT);
			if(mappings != null) {
				injector.applyMappings(mappings);
			}
			injector.mergeWith(serverStorage);
		}
		else {
			path = MCPPaths.get(mcp, JAR_ORIGINAL, side);
			injector.setStorage(new ClassStorage(IOUtil.readJar(path)));
			injector.addResources(path);
			if(stripGenerics) {
				injector.stripLVT();
				injector.addTransform(Transform::stripSignatures);
			}
			mappings = getMappings(mappingsPath, injector.getStorage(), side);
			if(mappings != null) {
				injector.applyMappings(mappings);
			}
		}
		injector.addTransform(Transform::decomposeVars);
		injector.addTransform(Transform::replaceCommonConstants);
		if(hasLWJGL) injector.addVisitor(new GLConstants(null));
		injector.restoreSourceFile();
		injector.fixInnerClasses();
		injector.fixImplicitConstructors();
		if(guessGenerics) injector.guessGenerics();
		final Path exc = MCPPaths.get(mcp, EXC);
		if (Files.exists(exc)) {
			injector.fixExceptions(exc);
		}
		if(side == Side.MERGED) {
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

	public static Mappings getMappings(Path mappingsPath, ClassStorage storage, Side side) throws IOException {
		if(!Files.exists(mappingsPath)) {
			return null;
		}
		boolean joined = MappingUtil.readNamespaces(mappingsPath).contains("official");
		Mappings mappings = MappingsIO.read(mappingsPath, joined ? "official" : side.name, "named");
		for(String name : storage.getAllClasses()) {
			if(name.indexOf('/') == -1 && !mappings.classes.containsKey(name)) {
				mappings.classes.put(name, "net/minecraft/src/" + name);
			}
		}
		return mappings;
	}

	public static String getLaunchArgs(MCP mcp, Side side) {
		List<String> args = TaskRun.getLaunchArgs(mcp, side);
		for(int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			if(arg.contains(" ")) {
				arg = "\"" + arg + "\"";
			}
			args.set(i, arg);
		}
		return String.join(" ", args).replace("\"", "&quot;");
	}

	public static void createProject(MCP mcp, Side side, int sourceVersion) throws IOException {
		Path proj = MCPPaths.get(mcp, PROJECT, side);
		Version version = mcp.getCurrentVersion();
		String clientArgs = getLaunchArgs(mcp, side);
		Side[] launchSides = side == Side.MERGED ? new Side[]{Side.CLIENT, Side.SERVER} : new Side[]{side};
		
		String projectName = "Minecraft " +
				( side == Side.CLIENT ? "Client"
				: side == Side.SERVER ? "Server"
				: side == Side.MERGED ? "Merged"
				: "Project");

		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(".classpath")))) {
			writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.startAttribute("classpath");
				writer.startAttribute("classpathentry kind=\"src\" path=\"src\"");
					writer.startAttribute("attributes");
						writer.writeAttribute("attribute name=\"org.eclipse.jdt.launching.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY\" value=\"" + projectName + "/libraries/natives\"");
					writer.closeAttribute("attributes");
				writer.closeAttribute("classpathentry");
				for(DependDownload dependencyDownload : version.libraries) {
					if(Rule.apply(dependencyDownload.rules)) {
						String[] path = dependencyDownload.name.split(":");
						String lib = path[0].replace('.', '/') + "/" + path[1] + "/" + path[2] + "/" + path[1] + "-" + path[2];
						String src = null;
						if(dependencyDownload.downloads != null && dependencyDownload.downloads.classifiers != null && dependencyDownload.downloads.classifiers.sources != null) {
							src = dependencyDownload.downloads.classifiers.sources.path;
						}
						if(Files.exists(MCPPaths.get(mcp, "libraries/" + lib + ".jar"))) {
							if(src != null) {
								writer.writeAttribute("classpathentry kind=\"lib\" path=\"libraries/" + lib + ".jar\" sourcepath=\"libraries/" + src + "\"");
							} else {
								writer.writeAttribute("classpathentry kind=\"lib\" path=\"libraries/" + lib + ".jar\"");
							}
						}
					}
				}
				writer.writeAttribute("classpathentry kind=\"lib\" path=\"jars/deobfuscated.jar\" sourcepath=\"jars/deobfuscated-source.jar\"");
				writer.writeAttribute("classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"");
				writer.writeAttribute("classpathentry kind=\"output\" path=\"output\"");
			writer.closeAttribute("classpath");
		}

		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(".project")))) {
			writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.startAttribute("projectDescription");
				writer.stringAttribute("name", projectName);
				writer.stringAttribute("comment", "");
				writer.startAttribute("projects");
				writer.closeAttribute("projects");
				writer.startAttribute("buildSpec");
					writer.startAttribute("buildCommand");
						writer.stringAttribute("name", "org.eclipse.jdt.core.javabuilder");
						writer.startAttribute("arguments");
						writer.closeAttribute("arguments");
					writer.closeAttribute("buildCommand");
				writer.closeAttribute("buildSpec");
				writer.startAttribute("natures");
					writer.stringAttribute("nature", "org.eclipse.jdt.core.javanature");
				writer.closeAttribute("natures");
				writer.startAttribute("linkedResources");
					writer.startAttribute("link");
						writer.stringAttribute("name", "libraries");
						writer.stringAttribute("type", "2");
						writer.stringAttribute("locationURI", "$%7BPARENT-1-PROJECT_LOC%7D/libraries");
					writer.closeAttribute("link");
				writer.closeAttribute("linkedResources");
				// Filter out src and jars
				long id = new Random().nextLong();
				writer.startAttribute("filteredResources");
				String[] matches = {"src", "jars", "source"};
				for(int i = 0; i < matches.length; i++) {
					writer.startAttribute("filter");
						writer.stringAttribute("id", Long.toString(id++));
						writer.stringAttribute("name", "");
						writer.stringAttribute("type", "9");
						writer.startAttribute("matcher");
							writer.stringAttribute("id", "org.eclipse.ui.ide.multiFilter");
							writer.stringAttribute("arguments", "1.0-name-matches-false-false-" + matches[i]);
						writer.closeAttribute("matcher");
					writer.closeAttribute("filter");
				}
				writer.closeAttribute("filteredResources");
			writer.closeAttribute("projectDescription");
		}

		for(Side launchSide : launchSides) {
			try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(Util.firstUpperCase(launchSide.name) + ".launch")))) {
				writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
				writer.startAttribute("launchConfiguration type=\"org.eclipse.jdt.launching.localJavaApplication\"");
					writer.startAttribute("listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\"");
						writer.writeAttribute("listEntry value=\"/"+ projectName +"\"");
					writer.closeAttribute("listAttribute");
					writer.startAttribute("listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_TYPES\"");
						writer.writeAttribute("listEntry value=\"4\"");
					writer.closeAttribute("listAttribute");
					writer.startAttribute("listAttribute key=\"org.eclipse.debug.ui.favoriteGroups\"");
						writer.writeAttribute("listEntry value=\"org.eclipse.debug.ui.launchGroup.run\"");
						writer.writeAttribute("listEntry value=\"org.eclipse.debug.ui.launchGroup.debug\"");
					writer.closeAttribute("listAttribute");
					writer.writeAttribute("booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_ATTR_USE_ARGFILE\" value=\"false\"");
					writer.writeAttribute("booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_SHOW_CODEDETAILS_IN_EXCEPTION_MESSAGES\" value=\"true\"");
					writer.writeAttribute("booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD\" value=\"true\"");
					writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.MAIN_TYPE\" value=\"" + TaskRun.getMain(mcp, mcp.getCurrentVersion(), launchSide) + "\"");
					writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.MODULE_NAME\" value=\""+ projectName +"\"");
					if(launchSide == Side.CLIENT) {
						writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.PROGRAM_ARGUMENTS\" value=\""+ clientArgs +"\"");
					}
					writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.PROJECT_ATTR\" value=\""+ projectName +"\"");
				writer.closeAttribute("launchConfiguration");
			}
		}

		Path settings = proj.resolve(".settings");
		Files.createDirectories(settings);

		String sourceVer = sourceVersion >= 9 ? String.valueOf(sourceVersion) : "1." + sourceVersion;

		try (BufferedWriter writer = Files.newBufferedWriter(settings.resolve("org.eclipse.jdt.core.prefs"))) {
			writer.write("eclipse.preferences.version=1"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled");
			writer.write("org.eclipse.jdt.core.compiler.codegen.methodParameters=do not generate"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.codegen.targetPlatform=" + sourceVer); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.codegen.unusedLocal=preserve"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.compliance=" + sourceVer); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.debug.lineNumber=generate"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.debug.localVariable=generate"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.debug.sourceFile=generate"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.problem.assertIdentifier=error"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.problem.enablePreviewFeatures=disabled"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.problem.enumIdentifier=error"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.problem.reportPreviewFeatures=warning"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.release=disabled"); writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.source=" + sourceVer); writer.newLine();
		}
	}

	@Override
	public void setProgress(int progress) {
		switch (step) {
		case STAGE_DECOMPILE: {
			int percent = (int)(progress * 0.82D);
			super.setProgress(3 + percent);
			break;
		}
		case STAGE_MD5: {
			int percent = (int)(progress * 0.04D);
			super.setProgress(96 + percent);
			break;
		}
		default:
			super.setProgress(progress);
			break;
		}
	}
}
