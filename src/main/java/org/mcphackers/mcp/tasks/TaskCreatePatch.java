package org.mcphackers.mcp.tasks;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;

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
		if (diffOperation.operate().exit != 0) {
			//addMessage(logger.toString(), Task.ERROR);
			//throw new Exception("Patches could not be created!");
		}
	}
}
