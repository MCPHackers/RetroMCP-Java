package org.mcphackers.mcp.tools.project.eclipse;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
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
		if (!side.equals(Side.SERVER)) {
			for (DependDownload dependencyDownload : this.dependencies) {
				if (Rule.apply(dependencyDownload.rules)) {
					String lib = dependencyDownload.getArtifactPath(null);
					if(lib == null) {
						continue;
					}
					String src = dependencyDownload.getArtifactPath("sources");
					if (Files.exists(MCPPaths.get(mcp, "libraries/" + lib))) {
						if (src != null) {
							writer.writeAttribute("classpathentry kind=\"lib\" path=\"libraries/" + lib + "\" sourcepath=\"libraries/" + src + "\"");
						} else {
							writer.writeAttribute("classpathentry kind=\"lib\" path=\"libraries/" + lib + "\"");
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
