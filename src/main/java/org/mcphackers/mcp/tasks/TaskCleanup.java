package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
import org.mcphackers.mcp.tools.MCPPaths;
import org.mcphackers.mcp.tools.Util;

public class TaskCleanup extends Task {

	public TaskCleanup(MCP instance) {
		super(Side.ANY, instance);
	}

	@Override
	public void doTask() throws Exception {
		cleanup(mcp.getOptions().getBooleanParameter(TaskParameter.SRC_CLEANUP));
	}
	
	public void cleanup(boolean srcCleanup) throws Exception {
		long startTime = System.currentTimeMillis();
		int foldersDeleted = 0;
		Path[] pathsToDelete = new Path[] {
				MCPPaths.get(mcp, MCPPaths.CONF),
				MCPPaths.get(mcp, MCPPaths.JARS),
				MCPPaths.get(mcp, MCPPaths.LIB),
				MCPPaths.get(mcp, MCPPaths.LIB_CLIENT),
				MCPPaths.get(mcp, MCPPaths.LIB_SERVER),
				MCPPaths.get(mcp, MCPPaths.TEMP),
				MCPPaths.get(mcp, MCPPaths.SRC),
				MCPPaths.get(mcp, MCPPaths.BIN),
				MCPPaths.get(mcp, MCPPaths.REOBF),
				MCPPaths.get(mcp, MCPPaths.BUILD),
				MCPPaths.get(mcp, "workspace")
			};
		if (srcCleanup) pathsToDelete = new Path[] {
				MCPPaths.get(mcp, MCPPaths.SRC),
				MCPPaths.get(mcp, MCPPaths.BIN),
				MCPPaths.get(mcp, MCPPaths.REOBF),
				MCPPaths.get(mcp, MCPPaths.BUILD)
			};
		for (Path path : pathsToDelete) {
			if (Files.exists(path)) {
				foldersDeleted++;
				log("Deleting " + path.getFileName() + "...");
				FileUtil.deleteDirectory(path);
			}
		}
		if(!srcCleanup) mcp.setCurrentVersion(null);

		if(foldersDeleted > 0) {
			log("Done in " + Util.time(System.currentTimeMillis() - startTime));
		}
		else {
			log("Nothing to clear!");
		}
	}
}
