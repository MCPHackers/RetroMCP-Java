package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;

public class TaskCleanup extends Task {
	
	private static final DecimalFormat DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	public TaskCleanup(MCP instance) {
		super(Side.ANY, instance);
	}

	@Override
	public void doTask() throws Exception {
		cleanup(mcp.getOptions().getBooleanParameter(TaskParameter.SRC_CLEANUP));
	}
	
	public void cleanup(boolean srcCleanup) throws Exception {
		Instant startTime = Instant.now();

		if (Files.exists(MCPPaths.get(mcp, "src/main/java/org/mcphackers"))) {
			throw new IllegalStateException("RMCP attempted to perform suicide. (Probably because you ran this application in the wrong folder)");
		}

		int foldersDeleted = 0;
		Path[] pathsToDelete = new Path[] {
				MCPPaths.get(mcp, MCPPaths.CONF),
				MCPPaths.get(mcp, MCPPaths.JARS),
				MCPPaths.get(mcp, MCPPaths.LIB),
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
			log("Done in " + DECIMAL.format(Duration.between(startTime, Instant.now()).get(ChronoUnit.NANOS) / 1e+9F) + "s");
		}
		else {
			log("Nothing to clear!");
		}
	}
}
