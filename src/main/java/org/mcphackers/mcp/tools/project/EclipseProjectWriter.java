package org.mcphackers.mcp.tools.project;

import static org.mcphackers.mcp.MCPPaths.PROJECT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.project.eclipse.EclipseClasspath;
import org.mcphackers.mcp.tools.project.eclipse.EclipsePreferences;
import org.mcphackers.mcp.tools.project.eclipse.EclipseProject;
import org.mcphackers.mcp.tools.project.eclipse.EclipseRunConfig;
import org.mcphackers.mcp.tools.versions.json.DependDownload;
import org.mcphackers.mcp.tools.versions.json.Version;

public class EclipseProjectWriter implements ProjectWriter {

	@Override
	public void createProject(MCP mcp, Side side, int sourceVersion) throws IOException {
		Path proj = MCPPaths.get(mcp, PROJECT, side);
		Version version = mcp.getCurrentVersion();
		String clientArgs = ProjectWriter.getLaunchArgs(mcp, side);
		Task.Side[] launchSides = side == Task.Side.MERGED ? new Task.Side[]{Task.Side.CLIENT, Task.Side.SERVER} : new Task.Side[]{side};

		String projectName = "Minecraft " + (side == Task.Side.CLIENT ? "Client" : side == Task.Side.SERVER ? "Server" : side == Task.Side.MERGED ? "Merged" : "Project");

		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(".classpath")))) {
			EclipseClasspath classpath = new EclipseClasspath(mcp, projectName);
			for (DependDownload dependency : version.libraries) {
				classpath.addDependency(dependency);
			}
			classpath.toXML(writer, side);
		}

		try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(".project")))) {
			EclipseProject project = new EclipseProject(projectName);
			project.toXML(writer);
		}

		for (Task.Side launchSide : launchSides) {
			try (XMLWriter writer = new XMLWriter(Files.newBufferedWriter(proj.resolve(Util.firstUpperCase(launchSide.name) + ".launch")))) {
				EclipseRunConfig runConfig = new EclipseRunConfig(mcp);
				runConfig.setProjectName(projectName);
				runConfig.setLaunchSide(launchSide);
				runConfig.setClientArgs(clientArgs);
				runConfig.toXML(writer);
			}
		}

		Path settings = proj.resolve(".settings");
		Files.createDirectories(settings);

		String sourceVer = sourceVersion >= 9 ? String.valueOf(sourceVersion) : "1." + sourceVersion;

		try (PairWriter writer = new PairWriter(Files.newBufferedWriter(settings.resolve("org.eclipse.jdt.core.prefs")))) {
			EclipsePreferences preferences = new EclipsePreferences();
			preferences.setSourceVersion(sourceVer);
			preferences.toString(writer);
		}
	}
}
