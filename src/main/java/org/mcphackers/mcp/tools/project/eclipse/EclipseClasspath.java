package org.mcphackers.mcp.tools.project.eclipse;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tools.project.XMLWriter;
import org.mcphackers.mcp.tools.versions.json.DependDownload;
import org.mcphackers.mcp.tools.versions.json.Rule;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class EclipseClasspath {
	private final MCP mcp;
	private final String projectName;
	private final List<DependDownload> dependencies = new ArrayList<>();

	public EclipseClasspath(MCP mcp, String projectName) {
		this.mcp = mcp;
		this.projectName = projectName;
	}

	public void addDependency(DependDownload dependency) {
		this.dependencies.add(dependency);
	}

	public void toXML(XMLWriter writer, Task.Side side) throws IOException {
		writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.startAttribute("classpath");
		writer.startAttribute("classpathentry kind=\"src\" path=\"src\"");
		writer.startAttribute("attributes");
		writer.writeAttribute("attribute name=\"org.eclipse.jdt.launching.CLASSPATH_ATTR_LIBRARY_PATH_ENTRY\" value=\"" + projectName + "/libraries/natives\"");
		writer.closeAttribute("attributes");
		writer.closeAttribute("classpathentry");
		// TODO: Implement proper client/server side libraries
		if (!side.equals(Task.Side.SERVER)) {
			for (DependDownload dependencyDownload : this.dependencies) {
				if (Rule.apply(dependencyDownload.rules)) {
					String[] path = dependencyDownload.name.split(":");
					String lib = path[0].replace('.', '/') + "/" + path[1] + "/" + path[2] + "/" + path[1] + "-" + path[2];
					String src = null;
					if (dependencyDownload.downloads != null && dependencyDownload.downloads.classifiers != null && dependencyDownload.downloads.classifiers.sources != null) {
						src = dependencyDownload.downloads.classifiers.sources.path;
					}
					if (Files.exists(MCPPaths.get(this.mcp, "libraries/" + lib + ".jar"))) {
						if (src != null) {
							writer.writeAttribute("classpathentry kind=\"lib\" path=\"libraries/" + lib + ".jar\" sourcepath=\"libraries/" + src + "\"");
						} else {
							writer.writeAttribute("classpathentry kind=\"lib\" path=\"libraries/" + lib + ".jar\"");
						}
					}
				}
			}
		}
		writer.writeAttribute("classpathentry kind=\"lib\" path=\"jars/deobfuscated.jar\" sourcepath=\"jars/deobfuscated-source.jar\"");
		writer.writeAttribute("classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\"");
		writer.writeAttribute("classpathentry kind=\"output\" path=\"output\"");
		writer.closeAttribute("classpath");
	}
}
