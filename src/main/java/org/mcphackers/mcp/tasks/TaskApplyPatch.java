package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.PATCHES;
import static org.mcphackers.mcp.MCPPaths.SOURCE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import codechicken.diffpatch.PatchOperation;
import codechicken.diffpatch.util.PatchMode;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;

public class TaskApplyPatch extends TaskStaged {

	public TaskApplyPatch(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[] {
				stage(getLocalizedStage("patching"), 0, () -> {
					final Path patchesPath = MCPPaths.get(mcp, PATCHES, side);
					final Path srcPath = MCPPaths.get(mcp, SOURCE, side);
					patch(this, srcPath, srcPath, patchesPath);
				})
		};
	}

	public static void patch(Task task, Path base, Path out, Path patches) throws IOException {
		ByteArrayOutputStream logger = new ByteArrayOutputStream();
		PatchOperation patchOperation = PatchOperation.builder()
				.basePath(base)
				.patchesPath(patches)
				.outputPath(out)
				.mode(PatchMode.OFFSET)
				.build();
		boolean success = patchOperation.doPatch();
		patchOperation.getSummary().print(new PrintStream(logger), false);
		if (!success) {
			task.addMessage(logger.toString(), Task.INFO);
			task.addMessage("Patching failed!", Task.ERROR);
		}
	}
}
