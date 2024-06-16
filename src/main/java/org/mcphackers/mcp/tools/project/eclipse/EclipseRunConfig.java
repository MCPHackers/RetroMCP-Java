package org.mcphackers.mcp.tools.project.eclipse;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskRun;
import org.mcphackers.mcp.tools.project.XMLWriter;

import java.io.IOException;

public class EclipseRunConfig {
	private final MCP mcp;
	private String projectName;
	private Task.Side launchSide;
	private String clientArgs;

	public EclipseRunConfig(MCP mcp) {
		this.mcp = mcp;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return this.projectName;
	}

	public void setLaunchSide(Task.Side launchSide) {
		this.launchSide = launchSide;
	}

	public Task.Side getLaunchSide() {
		return this.launchSide;
	}

	public void setClientArgs(String clientArgs) {
		this.clientArgs = clientArgs;
	}

	public String getClientArgs() {
		return this.clientArgs;
	}

	public void toXML(XMLWriter writer) throws IOException {
		writer.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
		writer.startAttribute("launchConfiguration type=\"org.eclipse.jdt.launching.localJavaApplication\"");
		writer.startAttribute("listAttribute key=\"org.eclipse.debug.core.MAPPED_RESOURCE_PATHS\"");
		writer.writeAttribute("listEntry value=\"/" + this.getProjectName() + "\"");
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
		writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.MAIN_TYPE\" value=\"" + TaskRun.getMain(mcp, mcp.getCurrentVersion(), this.getLaunchSide()) + "\"");
		writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.MODULE_NAME\" value=\"" + this.getProjectName() + "\"");
		if (launchSide == Task.Side.CLIENT) {
			writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.PROGRAM_ARGUMENTS\" value=\"" + this.getClientArgs() + "\"");
		}
		writer.writeAttribute("stringAttribute key=\"org.eclipse.jdt.launching.PROJECT_ATTR\" value=\"" + this.getProjectName() + "\"");
		writer.closeAttribute("launchConfiguration");
	}
}
