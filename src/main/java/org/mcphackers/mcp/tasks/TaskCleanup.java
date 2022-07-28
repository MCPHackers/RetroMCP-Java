package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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

		boolean deleted = false;
		List<Path> filesToDelete = new ArrayList<>();
		for(Side side : new Side[] {Side.CLIENT, Side.SERVER, Side.MERGED}) {
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.TEMP_SIDE, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.REMAPPED, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.TEMP_SRC, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.MD5, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.MD5_RO, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.REOBF_JAR, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.PATCHES, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.SOURCE, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.COMPILED, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.REOBF_SIDE, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.BUILD_ZIP, side));
			filesToDelete.add(MCPPaths.get(mcp, MCPPaths.BUILD_JAR, side));
		}
		filesToDelete.add(MCPPaths.get(mcp, MCPPaths.WORKSPACE));
		filesToDelete.add(MCPPaths.get(mcp, MCPPaths.CONF));
		filesToDelete.add(MCPPaths.get(mcp, MCPPaths.LIB));
		filesToDelete.add(MCPPaths.get(mcp, MCPPaths.NATIVES));
		
		List<Path> foldersToDelete = Arrays.asList(new Path[] {
				MCPPaths.get(mcp, MCPPaths.JARS),
				MCPPaths.get(mcp, MCPPaths.TEMP),
				MCPPaths.get(mcp, MCPPaths.SRC),
				MCPPaths.get(mcp, MCPPaths.BIN),
				MCPPaths.get(mcp, MCPPaths.REOBF),
				MCPPaths.get(mcp, MCPPaths.BUILD),
			});
		for(Path path : filesToDelete) {
			if(Files.exists(path)) {
				deleted |= true;
				FileUtil.delete(path);
			}
		}
		for(Path path : foldersToDelete) {
			if(Files.exists(path) && path.toFile().list().length == 0) {
				deleted |= true;
				Files.delete(path);
			}
		}
		mcp.setCurrentVersion(null);

		if(deleted) {
			log("Cleanup finished in " + DECIMAL.format(Duration.between(startTime, Instant.now()).get(ChronoUnit.NANOS) / 1e+9F) + "s");
		}
		else {
			log("Nothing to clear!");
		}
	}
}
