package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;

public class TaskCleanup extends Task {

	public TaskCleanup(MCP mcp) {
		super(0, mcp);
	}

	@Override
	public void doTask() throws Exception {
		long startTime = System.currentTimeMillis();
		int foldersDeleted = 0;
		Path[] pathsToDelete = new Path[] {
				Paths.get(MCPPaths.CONF),
				Paths.get(MCPPaths.JARS),
				Paths.get(MCPPaths.LIB),
				Paths.get(MCPPaths.TEMP),
				Paths.get(MCPPaths.SRC),
				Paths.get(MCPPaths.BIN),
				Paths.get(MCPPaths.REOBF),
				Paths.get(MCPPaths.BUILD),
				Paths.get("workspace")
			};
		if (mcp.getBooleanParam(TaskParameter.SRC_CLEANUP)) pathsToDelete = new Path[] {
				Paths.get(MCPPaths.SRC),
				Paths.get(MCPPaths.BIN),
				Paths.get(MCPPaths.REOBF),
				Paths.get(MCPPaths.BUILD)
			};
		for (Path path : pathsToDelete) {
			if (Files.exists(path)) {
				foldersDeleted++;
				mcp.log(" Deleting " + path + "...");
				FileUtil.deleteDirectory(path);
			}
		}

		if(foldersDeleted > 0) {
			mcp.log(" Done in " + Util.time(System.currentTimeMillis() - startTime));
		}
		else {
			mcp.log(" Nothing to clear!");
		}
	}
}
