package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;

public class TaskMergeMappings extends TaskStaged {
	public TaskMergeMappings(MCP instance) {
		super(instance);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[] {
				stage(getLocalizedStage("mergemappings"), () -> {

				})
		};
	}
}
