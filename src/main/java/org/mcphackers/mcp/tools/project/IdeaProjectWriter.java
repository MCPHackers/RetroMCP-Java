package org.mcphackers.mcp.tools.project;

import static org.mcphackers.mcp.MCPPaths.PROJECT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.TaskRun;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.versions.json.DependDownload;
import org.mcphackers.mcp.tools.versions.json.Rule;
import org.mcphackers.mcp.tools.versions.json.Version;

public class IdeaProjectWriter implements ProjectWriter {
	@Override
	public void createProject(MCP mcp, Side side, int sourceVersion) throws IOException {
		Path proj = MCPPaths.get(mcp, PROJECT, side);
		Version version = mcp.getCurrentVersion();
		String clientArgs = ProjectWriter.getLaunchArgs(mcp, side);
		Path ideaFolder = proj.resolve(".idea");
		Path librariesFolder = ideaFolder.resolve("libraries");
		FileUtil.createDirectories(ideaFolder);
		FileUtil.createDirectories(librariesFolder);

		Path modulesXML = ideaFolder.resolve("modules.xml");
		Path workspaceXML = ideaFolder.resolve("workspace.xml");
		Path miscXML = ideaFolder.resolve("misc.xml");

		String projectName = "Minecraft " + (side == Task.Side.CLIENT ? "Client" : side == Task.Side.SERVER ? "Server" : side == Task.Side.MERGED ? "Merged" : "Project");

		String moduleName = projectName.toLowerCase().replace(" ", "_");
		this.writeProjectIML(mcp, version, moduleName, proj);
		this.writeModuleXML(mcp, moduleName, modulesXML);
		this.writeMiscXML(miscXML);
		// TODO: Support for server args?
		this.writeWorkspace(mcp, side, projectName, clientArgs, workspaceXML);
		this.writeLibraries(mcp, proj, version);
	}

	public void writeProjectIML(MCP mcp, Version version, String moduleName, Path proj) throws IOException {
		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(moduleName + ".iml")))) {
			writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.startAttribute("module type=\"JAVA_MODULE\" version=\"4\"");
			writer.startAttribute("component name=\"NewModuleRootManager\" inherit-compiler-output=\"true\"");

			writer.writeSelfEndingAttribute("exclude-output");

			writer.startAttribute("content url=\"file://$MODULE_DIR$\"");
			writer.writeSelfEndingAttribute("sourceFolder url=\"file://$MODULE_DIR$/src\" isTestSource=\"false\"");
			writer.closeAttribute("content");

			writer.writeSelfEndingAttribute("orderEntry type=\"inheritedJdk\"");
			writer.writeSelfEndingAttribute("orderEntry type=\"sourceFolder\" forTests=\"false\"");
			for (DependDownload dependencyDownload : version.libraries) {
				if (Rule.apply(dependencyDownload.rules)) {
					String[] path = dependencyDownload.name.split(":");
					String lib = path[0].replace('.', '/') + "/" + path[1] + "/" + path[2] + "/" + path[1] + "-" + path[2];
					Path libPath = Paths.get(lib);
					String libraryName = libPath.getFileName().toString();
					if (Files.exists(MCPPaths.get(mcp, "libraries/" + lib + ".jar"))) {
						writer.writeSelfEndingAttribute("orderEntry type=\"library\" name=\"" + libraryName + "\" level=\"project\"");
					}
				}
			}
			writer.writeSelfEndingAttribute("orderEntry type=\"library\" name=\"deobfuscated\" level=\"project\"");

			writer.closeAttribute("component");
			writer.closeAttribute("module");
		}
	}

	public void writeModuleXML(MCP mcp, String moduleName, Path modulesXML) throws IOException {
		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(modulesXML))) {
			writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.startAttribute("project version=\"4\"");

			writer.startAttribute("component name=\"ProjectModuleManager\"");
			writer.startAttribute("modules");
			writer.writeSelfEndingAttribute("module fileurl=\"file://$PROJECT_DIR$/" + moduleName + ".iml\" filepath=\"$PROJECT_DIR/" + moduleName + ".iml\"");
			writer.closeAttribute("modules");
			writer.closeAttribute("component");

			writer.closeAttribute("project");
		}
	}

	public void writeMiscXML(Path miscXML) throws IOException {
		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(miscXML))) {
			writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.startAttribute("project version=\"4\"");
			writer.startAttribute("component name=\"ProjectRootManager\" version=\"2\" languageLevel=\"JDK_1_8\" default=\"true\" project-jdk-name=\"1.8\" project-jdk-type=\"JavaSDK\"");
			writer.writeSelfEndingAttribute("output url=\"file://$PROJECT_DIR$/output\"");
			writer.closeAttribute("component");
			writer.closeAttribute("project");
		}
	}

	public void writeWorkspace(MCP mcp, Task.Side launchSide, String projectName, String args, Path workspaceXML) throws IOException {
		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(workspaceXML))) {
			writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.startAttribute("project version=\"4\"");

			// Run configurations
			writer.startAttribute("component name=\"RunManager\"");

			writer.startAttribute("configuration name=\"" + projectName + "\" type=\"Application\" factoryName=\"Application\"");
			writer.writeSelfEndingAttribute("option name=\"MAIN_CLASS_NAME\" value=\"" + TaskRun.getMain(mcp, mcp.getCurrentVersion(), launchSide) + "\"");
			writer.writeSelfEndingAttribute("module name=\"" + projectName.toLowerCase().replace(" ", "_") + "\"");
			writer.writeSelfEndingAttribute("option name=\"PROGRAM_PARAMETERS\" value=\"" + args + "\"");
			writer.writeSelfEndingAttribute("option name=\"WORKING_DIRECTORY\" value=\"$PROJECT_DIR$/game\"");
			writer.startAttribute("method v=\"2\"");
			writer.writeSelfEndingAttribute("option name=\"Make\" enabled=\"true\"");
			writer.closeAttribute("method");
			writer.closeAttribute("configuration");

			writer.closeAttribute("component");
			writer.closeAttribute("project");
		}
	}

	public void writeLibraries(MCP mcp, Path projectFolder, Version version) throws IOException {
		Path librariesFolder = projectFolder.resolve(".idea/libraries");
		// Write library XML files
		for (DependDownload dependencyDownload : version.libraries) {
			if (Rule.apply(dependencyDownload.rules)) {
				String[] path = dependencyDownload.name.split(":");
				String lib = path[0].replace('.', '/') + "/" + path[1] + "/" + path[2] + "/" + path[1] + "-" + path[2];
				String src = null;
				if (dependencyDownload.downloads != null && dependencyDownload.downloads.classifiers != null && dependencyDownload.downloads.classifiers.sources != null) {
					src = dependencyDownload.downloads.classifiers.sources.path;
				}
				if (Files.exists(MCPPaths.get(mcp, "libraries/" + lib + ".jar"))) {
					String libraryName = lib.substring(lib.lastIndexOf("/") + 1);
					Path libraryXML = librariesFolder.resolve(libraryName + ".xml");
					Files.createFile(libraryXML);
					try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(libraryXML))) {
						// No XML header???
						writer.startAttribute("component name=\"libraryTable\"");
						writer.startAttribute("library name=\"" + libraryName + "\"");

						writer.startAttribute("CLASSES");
						writer.writeSelfEndingAttribute("root url=\"jar://$PROJECT_DIR$/../libraries/" + lib + ".jar!/\"");
						writer.closeAttribute("CLASSES");
						writer.writeSelfEndingAttribute("JAVADOC");
						if (src != null) {
							writer.startAttribute("SOURCES");
							writer.writeSelfEndingAttribute("root url=\"jar://$PROJECT_DIR$/../libraries/" + src + "!/\"");
							writer.closeAttribute("SOURCES");
						}


						writer.closeAttribute("library");
						writer.closeAttribute("component");
					}
				}
			}
		}
		// Write deobfuscated MC to classpath
		Path mcXml = librariesFolder.resolve("deobfuscated.xml");
		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(mcXml))) {
			writer.startAttribute("component name=\"libraryTable\"");
			writer.startAttribute("library name=\"deobfuscated\"");

			writer.startAttribute("CLASSES");
			writer.writeSelfEndingAttribute("root url=\"jar://$PROJECT_DIR$/jars/deobfuscated.jar!/\"");
			writer.closeAttribute("CLASSES");
			writer.writeSelfEndingAttribute("JAVADOC");
			writer.startAttribute("NATIVE");
			writer.writeSelfEndingAttribute("root url=\"file://$PROJECT_DIR$/../libraries/natives\"");
			writer.closeAttribute("NATIVE");
			writer.startAttribute("SOURCES");
			writer.writeSelfEndingAttribute("root url=\"jar://$PROJECT_DIR$/jars/deobfuscated-source.jar!/\"");
			writer.closeAttribute("SOURCES");


			writer.closeAttribute("library");
			writer.closeAttribute("component");
		}
	}
}
