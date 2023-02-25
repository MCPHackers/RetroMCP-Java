package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.PATCH;
import static org.mcphackers.mcp.MCPPaths.SOURCE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;

import codechicken.diffpatch.PatchOperation;
import codechicken.diffpatch.util.PatchMode;

public class TaskApplyPatch extends Task {

	public TaskApplyPatch(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		final Path patchesPath 	= MCPPaths.get(mcp, PATCH, side);
		final Path srcPath 		= MCPPaths.get(mcp, SOURCE, side);
		patch(this, srcPath, srcPath, patchesPath);
	}

	public static void patch(Task task, Path base, Path out, Path patches) throws IOException {
		ByteArrayOutputStream logger = new ByteArrayOutputStream();
		PatchOperation patchOperation = PatchOperation.builder()
				.basePath(base)
				.patchesPath(patches)
				.outputPath(out)
				.mode(PatchMode.OFFSET)
				.filter(p -> p.endsWith(".java"))
				.build();
		boolean success = patchOperation.doPatch();
		patchOperation.getSummary().print(new PrintStream(logger), false);
		if (!success) {
			task.addMessage(logger.toString(), Task.INFO);
			task.addMessage("Patching failed!", Task.ERROR);
		}
	}
}
