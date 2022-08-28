package org.mcphackers.mcp.tasks;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tools.FileUtil;

import codechicken.diffpatch.cli.DiffOperation;

public class TaskCreatePatch extends Task {
	public TaskCreatePatch(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		Path srcPathUnpatched = MCPPaths.get(mcp, MCPPaths.TEMP_SRC, side);
		Path srcPathPatched = MCPPaths.get(mcp, MCPPaths.SOURCE, side);
		Path patchesOut = MCPPaths.get(mcp, "patches/patches_%s", side);
		if(Files.isDirectory(patchesOut)) {
			FileUtil.cleanDirectory(patchesOut);
		}
		setProgress(getLocalizedStage("createpatch"));
		if (Files.exists(srcPathUnpatched)) {
			if(Files.exists(srcPathPatched)) {
				createDiffOperation(srcPathUnpatched, srcPathPatched, patchesOut);
			}
			else {
				throw new Exception("Patched " + side.name + " sources cannot be found!");
			}
		} else {
			throw new Exception("Unpatched " + side.name + " sources cannot be found!");
		}
	}

	public void createDiffOperation(Path aPath, Path bPath, Path outputPath) throws Exception {
		ByteArrayOutputStream logger = new ByteArrayOutputStream();
		DiffOperation diffOperation = DiffOperation.builder()
			.aPath(aPath)
			.bPath(bPath)
			.outputPath(outputPath)
			.verbose(true)
			.logTo(logger)
			.summary(true).build();
		diffOperation.operate();
	}
}
