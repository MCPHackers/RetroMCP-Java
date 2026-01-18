package org.mcphackers.mcp.tools.project.eclipse;

import java.io.IOException;
import java.util.Random;

import org.mcphackers.mcp.tools.project.XMLWriter;

public class EclipseProject {
	private final String projectName;

	public EclipseProject(String projectName) {
		this.projectName = projectName;
	}

	public void toXML(XMLWriter writer) throws IOException {
		writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.startAttribute("projectDescription");
		writer.stringAttribute("name", this.projectName);
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
		// Broken for VSCode even though it's supposedly eclipse-project compatible
		// String[] matches = {"src", "jars", "source"};
		// for (String match : matches) {
		// 	writer.startAttribute("filter");
		// 	writer.stringAttribute("id", Long.toString(id++));
		// 	writer.stringAttribute("name", "");
		// 	writer.stringAttribute("type", "9");
		// 	writer.startAttribute("matcher");
		// 	writer.stringAttribute("id", "org.eclipse.ui.ide.multiFilter");
		// 	writer.stringAttribute("arguments", "1.0-name-matches-false-false-" + match);
		// 	writer.closeAttribute("matcher");
		// 	writer.closeAttribute("filter");
		// }
		writer.closeAttribute("filteredResources");
		writer.closeAttribute("projectDescription");
	}
}
