package org.mcphackers.mcp.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;

import codechicken.diffpatch.DiffOperation;

public class TaskCreatePatch extends Task {
	public TaskCreatePatch(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		Path srcPathUnpatched = MCPPaths.get(mcp, MCPPaths.SOURCE_UNPATCHED, side);
		Path srcPathPatched = MCPPaths.get(mcp, MCPPaths.SOURCE, side);
		Path patchesOut = MCPPaths.get(mcp, MCPPaths.PATCH, side);
		setProgress(getLocalizedStage("createpatch"));
		if(!Files.exists(srcPathPatched)) {
			throw new IOException("Patched " + side.name + " sources cannot be found!");
		}
		if (!Files.exists(srcPathUnpatched)) {
			throw new IOException("Unpatched " + side.name + " sources cannot be found!");
		}
		createDiffOperation(srcPathUnpatched, srcPathPatched, patchesOut);
	}

	public boolean createDiffOperation(Path aPath, Path bPath, Path outputPath) throws Exception {
		DiffOperation diffOperation = DiffOperation.builder()
			.aPath(aPath)
			.bPath(bPath)
			.aPrefix(null)
			.bPrefix(null)
			.singleDiff(true)
			.outputPath(outputPath)
			.filter(p -> p.endsWith(".java"))
			.build();
		return diffOperation.doDiff();
	}
}
