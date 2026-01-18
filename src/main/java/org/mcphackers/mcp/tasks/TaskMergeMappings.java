package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tools.mappings.MappingUtil;

public class TaskMergeMappings extends TaskStaged {
	public TaskMergeMappings(MCP instance) {
		super(instance);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[]{
				stage(getLocalizedStage("mergemappings"), 0, () -> {
					Path clientMappings = Paths.get("client.tiny");
					Path serverMappings = Paths.get("server.tiny");
					Path mergedMappings = Paths.get("merged.tiny");
					if (Files.exists(clientMappings)) {
						if (Files.exists(serverMappings)) {
							// Merge client & server mappings
							MappingUtil.mergeMappings(clientMappings, serverMappings, mergedMappings);
						} else {
							// Only client mappings exist
							MappingUtil.mergeMappings(clientMappings, mergedMappings);
						}
					} else {
						throw new RuntimeException("client.tiny/server.tiny could not be found in the current directory!");
					}
				})
		};
	}
}
