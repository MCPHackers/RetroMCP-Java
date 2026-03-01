package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.ClassUtils;
import org.mcphackers.mcp.tools.project.EclipseProjectWriter;
import org.mcphackers.mcp.tools.project.IdeaProjectWriter;
import org.mcphackers.mcp.tools.project.VSCProjectWriter;

public class TaskGenerateProject extends TaskStaged {
	public TaskGenerateProject(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	protected Stage[] setStages() {
		int requestedJavaVersion = mcp.getOptions().getIntParameter(TaskParameter.TARGET_VERSION);
		int classVersion = Math.max(requestedJavaVersion, 52);

		return new Stage[]{
				stage(getLocalizedStage("create_eclipse_project", 0), () -> {
					new EclipseProjectWriter().createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
				}),
				stage(getLocalizedStage("create_idea_project"), () -> {
					new IdeaProjectWriter().createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
				}),
				stage(getLocalizedStage("create_vsc_project"), () -> {
					new VSCProjectWriter().createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
				})
		};
	}
}
