package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tools.ClassUtils;
import org.mcphackers.mcp.tools.project.EclipseProjectWriter;
import org.mcphackers.mcp.tools.project.IdeaProjectWriter;

public class TaskGenerateRunConfigs extends TaskStaged {
	public TaskGenerateRunConfigs(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[]{
				stage("generateRunConfig", 0, () -> {
					int classVersion = 8;
					new EclipseProjectWriter().createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
					new IdeaProjectWriter().createProject(mcp, side, ClassUtils.getSourceFromClassVersion(classVersion));
				})
		};
	}
}
