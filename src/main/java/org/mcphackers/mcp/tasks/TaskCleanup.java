package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.Util;

public class TaskCleanup extends Task {

	public TaskCleanup(MCP instance) {
		super(Side.NONE, instance);
	}

	@Override
	public void doTask() throws Exception {
		cleanup(false);
	}
	
	public void cleanup(boolean srcCleanup) throws Exception {
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
		if (srcCleanup) pathsToDelete = new Path[] {
				Paths.get(MCPPaths.SRC),
				Paths.get(MCPPaths.BIN),
				Paths.get(MCPPaths.REOBF),
				Paths.get(MCPPaths.BUILD)
			};
		for (Path path : pathsToDelete) {
			if (Files.exists(path)) {
				foldersDeleted++;
				log(" Deleting " + path + "...");
				FileUtil.deleteDirectory(path);
			}
		}

		if(foldersDeleted > 0) {
			log(" Done in " + Util.time(System.currentTimeMillis() - startTime));
		}
		else {
			log(" Nothing to clear!");
		}
	}

	@Override
	public String getName() {
		return "Cleanup";
	}
}
