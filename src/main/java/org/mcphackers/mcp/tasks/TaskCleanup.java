package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;

public class TaskCleanup extends Task {

	public TaskCleanup(TaskInfo info) {
		super(-1 , info);
	}

	@Override
	public void doTask() throws Exception {
		long startTime = System.currentTimeMillis();
		int foldersDeleted = 0;
		Path[] pathsToDelete = new Path[] {
				Paths.get(MCPConfig.CONF),
				Paths.get(MCPConfig.JARS),
				Paths.get(MCPConfig.LIB),
				Paths.get(MCPConfig.DEPS_C),
				Paths.get(MCPConfig.DEPS_S),
				Paths.get(MCPConfig.TEMP),
				Paths.get(MCPConfig.SRC),
				Paths.get(MCPConfig.BIN),
				Paths.get(MCPConfig.REOBF),
				Paths.get(MCPConfig.BUILD),
				Paths.get("workspace")
			};
		if (MCP.config.srcCleanup) pathsToDelete = new Path[] {
				Paths.get(MCPConfig.SRC),
				Paths.get(MCPConfig.BIN),
				Paths.get(MCPConfig.REOBF),
				Paths.get(MCPConfig.BUILD)
			};
		for (Path path : pathsToDelete) {
			if (Files.exists(path)) {
				foldersDeleted++;
				MCP.logger.info(" Deleting " + path + "...");
				FileUtil.deleteDirectory(path);
			}
		}

		if(foldersDeleted > 0) {
			MCP.logger.info(" Done in " + Util.time(System.currentTimeMillis() - startTime));
		}
		else {
			MCP.logger.info(" Nothing to clear!");
		}
	}
}
