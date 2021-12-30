package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.Util;

public class TaskCleanup extends Task {

    public TaskCleanup(TaskInfo info) {
		super(-1 , info);
	}

    @Override
    public void doTask() throws Exception {
        long startTime = System.nanoTime();
        int foldersDeleted = 0;
        Path[] pathsToDelete = new Path[] { Paths.get("jars"), Paths.get("temp"), Paths.get("src"), Paths.get("reobf"), Paths.get("eclipse"), Paths.get("bin")};
        if (MCPConfig.srcCleanup) pathsToDelete = new Path[] { Paths.get("src"), Paths.get("bin")};
        for (Path path : pathsToDelete) {
        	if (Files.exists(path)) {
        		foldersDeleted++;
        		MCP.logger.info("Deleting " + path + "...");
        		Util.deleteDirectory(path);
        	}
        }
        long endTime = System.nanoTime();

        long seconds = TimeUnit.SECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        long nanoSeconds = endTime - startTime;

        if(foldersDeleted > 0) {
        	MCP.logger.info("Done in " + (seconds == 0 ? nanoSeconds + " ns" : seconds + " s"));
        }
        else {
        	MCP.logger.info("Nothing to clear!");
        }
    }
}
