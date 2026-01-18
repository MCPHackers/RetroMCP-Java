package org.mcphackers.mcp.tools.project;

import static org.mcphackers.mcp.MCPPaths.PROJECT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(moduleName.replace("_client", "") + ".iml")))) {
			writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.startAttribute("module type=\"JAVA_MODULE\" version=\"4\"");
			writer.startAttribute("component name=\"NewModuleRootManager\" inherit-compiler-output=\"true\"");

			writer.writeSelfEndingAttribute("exclude-output");

			writer.startAttribute("content url=\"file://$MODULE_DIR$\"");
			writer.writeSelfEndingAttribute("sourceFolder url=\"file://$MODULE_DIR$/src\" isTestSource=\"false\"");
			writer.writeSelfEndingAttribute("excludeFolder url=\"file://$MODULE_DIR$/src_original\"");
			writer.closeAttribute("content");

			writer.writeSelfEndingAttribute("orderEntry type=\"inheritedJdk\"");
			writer.writeSelfEndingAttribute("orderEntry type=\"sourceFolder\" forTests=\"false\"");
			for (DependDownload dependencyDownload : version.libraries) {
				if (Rule.apply(dependencyDownload.rules)) {
					String lib = dependencyDownload.getArtifactPath(null);
					if (lib == null) {
						continue;
					}
					String libraryName = lib.substring(lib.lastIndexOf("/") + 1, lib.length() - 4);
					if (Files.exists(MCPPaths.get(mcp, "libraries/" + lib))) {
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
			String folderName = moduleName.replace("_client", "");
			writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.startAttribute("project version=\"4\"");

			writer.startAttribute("component name=\"ProjectModuleManager\"");
			writer.startAttribute("modules");
			writer.writeSelfEndingAttribute("module fileurl=\"file://$PROJECT_DIR$/" + folderName + ".iml\" filepath=\"$PROJECT_DIR$/" + folderName + ".iml\"");
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

			if (Side.MERGED.equals(launchSide)) {
				String client = "Minecraft Client";
				String server = "Minecraft Server";

				// Client run config
				writer.startAttribute("configuration name=\"" + client + "\" type=\"Application\" factoryName=\"Application\"");
				writer.writeSelfEndingAttribute("option name=\"MAIN_CLASS_NAME\" value=\"" + TaskRun.getMain(mcp, mcp.getCurrentVersion(), Side.CLIENT) + "\"");
				writer.writeSelfEndingAttribute("module name=\"" + projectName.toLowerCase().replace(" ", "_") + "\"");
				writer.writeSelfEndingAttribute("option name=\"PROGRAM_PARAMETERS\" value=\"" + args + "\"");
				writer.writeSelfEndingAttribute("option name=\"WORKING_DIRECTORY\" value=\"$PROJECT_DIR$/game\"");
				writer.startAttribute("method v=\"2\"");
				writer.writeSelfEndingAttribute("option name=\"Make\" enabled=\"true\"");
				writer.closeAttribute("method");
				writer.closeAttribute("configuration");

				// Server run config
				writer.startAttribute("configuration name=\"" + server + "\" type=\"Application\" factoryName=\"Application\"");
				writer.writeSelfEndingAttribute("option name=\"MAIN_CLASS_NAME\" value=\"" + TaskRun.getMain(mcp, mcp.getCurrentVersion(), Side.SERVER) + "\"");
				writer.writeSelfEndingAttribute("module name=\"" + projectName.toLowerCase().replace(" ", "_") + "\"");
				writer.writeSelfEndingAttribute("option name=\"PROGRAM_PARAMETERS\" value=\"" + "\"");
				writer.writeSelfEndingAttribute("option name=\"WORKING_DIRECTORY\" value=\"$PROJECT_DIR$/game\"");
				writer.startAttribute("method v=\"2\"");
				writer.writeSelfEndingAttribute("option name=\"Make\" enabled=\"true\"");
				writer.closeAttribute("method");
				writer.closeAttribute("configuration");

				writer.startAttribute("list");
				writer.writeSelfEndingAttribute("item itemvalue=\"Application." + client + "\"");
				writer.writeSelfEndingAttribute("item itemvalue=\"Application." + server + "\"");
				writer.closeAttribute("list");
			} else {
				writer.startAttribute("configuration name=\"" + projectName + "\" type=\"Application\" factoryName=\"Application\"");
				writer.writeSelfEndingAttribute("option name=\"MAIN_CLASS_NAME\" value=\"" + TaskRun.getMain(mcp, mcp.getCurrentVersion(), launchSide) + "\"");
				writer.writeSelfEndingAttribute("module name=\"" + projectName.toLowerCase().replace(" ", "_") + "\"");
				writer.writeSelfEndingAttribute("option name=\"PROGRAM_PARAMETERS\" value=\"" + args + "\"");
				writer.writeSelfEndingAttribute("option name=\"WORKING_DIRECTORY\" value=\"$PROJECT_DIR$/game\"");
				writer.startAttribute("method v=\"2\"");
				writer.writeSelfEndingAttribute("option name=\"Make\" enabled=\"true\"");
				writer.closeAttribute("method");
				writer.closeAttribute("configuration");
			}

			writer.closeAttribute("component");
			writer.closeAttribute("project");
		}
	}

	public void writeLibraries(MCP mcp, Path projectFolder, Version version) throws IOException {
		Path librariesFolder = projectFolder.resolve(".idea/libraries");
		// Write library XML files
		for (DependDownload dependencyDownload : version.libraries) {
			if (Rule.apply(dependencyDownload.rules)) {
				String lib = dependencyDownload.getArtifactPath(null);
				if (lib == null) {
					continue;
				}
				String src = dependencyDownload.getArtifactPath("sources");
				if (Files.exists(MCPPaths.get(mcp, "libraries/" + lib))) {
					String libraryName = lib.substring(lib.lastIndexOf("/") + 1, lib.length() - 4);
					Path libraryXML = librariesFolder.resolve(libraryName.replaceAll("-", "_").replaceAll("\\.", "_") + ".xml");
					Files.createFile(libraryXML);
					try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(libraryXML))) {
						// No XML header???
						writer.startAttribute("component name=\"libraryTable\"");
						writer.startAttribute("library name=\"" + libraryName + "\"");

						writer.startAttribute("CLASSES");
						writer.writeSelfEndingAttribute("root url=\"jar://$PROJECT_DIR$/../libraries/" + lib + "!/\"");
						writer.closeAttribute("CLASSES");
						writer.writeSelfEndingAttribute("JAVADOC");
						if (src != null) {
							writer.startAttribute("SOURCES");
							writer.writeSelfEndingAttribute("root url=\"jar://$PROJECT_DIR$/../libraries/" + src + "!/\"");
							writer.closeAttribute("SOURCES");
						} else {
							writer.writeSelfEndingAttribute("SOURCES");
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
