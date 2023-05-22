package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tools.FileUtil;

public class TaskCleanup extends Task {

	private static final DecimalFormat DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	public TaskCleanup(MCP instance) {
		super(Side.ANY, instance);
	}

	@Override
	public void doTask() throws Exception {
		Instant startTime = Instant.now();

		boolean deleted = cleanup();

		mcp.setCurrentVersion(null);

		if (deleted) {
			log("Cleanup finished in " + DECIMAL.format(Duration.between(startTime, Instant.now()).get(ChronoUnit.NANOS) / 1e+9F) + "s");
		} else {
			log("Nothing to clear!");
		}
	}

	public boolean cleanup() throws IOException {

		boolean deleted = false;
		List<Path> filesToDelete = new ArrayList<>();
		for (Side side : Side.ALL) {
			filesToDelete.add(MCPPaths.get(mcp, JAR_ORIGINAL, side));
			filesToDelete.add(MCPPaths.get(mcp, PROJECT, side));
			filesToDelete.add(MCPPaths.get(mcp, PATCHES, side));
			filesToDelete.add(MCPPaths.get(mcp, BUILD_ZIP, side));
			filesToDelete.add(MCPPaths.get(mcp, BUILD_JAR, side));
		}
		filesToDelete.add(MCPPaths.get(mcp, CONF));
		filesToDelete.add(MCPPaths.get(mcp, NATIVES));

		Path[] foldersToDelete = new Path[]{
				MCPPaths.get(mcp, JARS),
		};
		for (Path path : filesToDelete) {
			if (Files.exists(path)) {
				deleted = true;
				FileUtil.delete(path);
			}
		}
		for (Path path : foldersToDelete) {
			if (Files.exists(path) && path.toFile().list().length == 0) {
				deleted = true;
				Files.delete(path);
			}
		}
		return deleted;
	}
}
