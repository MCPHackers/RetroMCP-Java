package org.mcphackers.mcp.tools.project;

import static org.mcphackers.mcp.MCPPaths.PROJECT;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskRun;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.json.DependDownload;
import org.mcphackers.mcp.tools.versions.json.Rule;
import org.mcphackers.mcp.tools.versions.json.Version;

public class EclipseProjectWriter implements ProjectWriter {

	@Override
	public void createProject(MCP mcp, Side side, int sourceVersion) throws IOException {
		Path proj = MCPPaths.get(mcp, PROJECT, side);
		Version version = mcp.getCurrentVersion();
		String clientArgs = ProjectWriter.getLaunchArgs(mcp, side);
		Task.Side[] launchSides = side == Task.Side.MERGED ? new Task.Side[] { Task.Side.CLIENT, Task.Side.SERVER } : new Task.Side[] { side };

		String projectName = "Minecraft " + (side == Task.Side.CLIENT ? "Client" : side == Task.Side.SERVER ? "Server" : side == Task.Side.MERGED ? "Merged" : "Project");

		try(XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(".classpath")))) {
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

		try(XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(".project")))) {
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
			String[] matches = { "src", "jars", "source" };
			for (String match : matches) {
				writer.startAttribute("filter");
				writer.stringAttribute("id", Long.toString(id++));
				writer.stringAttribute("name", "");
				writer.stringAttribute("type", "9");
				writer.startAttribute("matcher");
				writer.stringAttribute("id", "org.eclipse.ui.ide.multiFilter");
				writer.stringAttribute("arguments", "1.0-name-matches-false-false-" + match);
				writer.closeAttribute("matcher");
				writer.closeAttribute("filter");
			}
			writer.closeAttribute("filteredResources");
			writer.closeAttribute("projectDescription");
		}

		for(Task.Side launchSide : launchSides) {
			try(XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(Util.firstUpperCase(launchSide.name) + ".launch")))) {
				writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
				writer.startAttribute("launchConfiguration type=\"org.eclipse.jdt.launching.localJavaApplication\"");
				writer.startAttribute("listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\"");
				writer.writeAttribute("listEntry value=\"/" + projectName + "\"");
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
				writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.MODULE_NAME\" value=\"" + projectName + "\"");
				if(launchSide == Task.Side.CLIENT) {
					writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.PROGRAM_ARGUMENTS\" value=\"" + clientArgs + "\"");
				}
				writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.PROJECT_ATTR\" value=\"" + projectName + "\"");
				writer.closeAttribute("launchConfiguration");
			}
		}

		Path settings = proj.resolve(".settings");
		Files.createDirectories(settings);

		String sourceVer = sourceVersion >= 9 ? String.valueOf(sourceVersion) : "1." + sourceVersion;

		try(BufferedWriter writer = Files.newBufferedWriter(settings.resolve("org.eclipse.jdt.core.prefs"))) {
			writer.write("eclipse.preferences.version=1");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled");
			writer.write("org.eclipse.jdt.core.compiler.codegen.methodParameters=do not generate");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.codegen.targetPlatform=" + sourceVer);
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.codegen.unusedLocal=preserve");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.compliance=" + sourceVer);
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.debug.lineNumber=generate");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.debug.localVariable=generate");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.debug.sourceFile=generate");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.problem.assertIdentifier=error");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.problem.enablePreviewFeatures=disabled");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.problem.enumIdentifier=error");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.problem.reportPreviewFeatures=warning");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.release=disabled");
			writer.newLine();
			writer.write("org.eclipse.jdt.core.compiler.source=" + sourceVer);
			writer.newLine();
		}
	}
}
