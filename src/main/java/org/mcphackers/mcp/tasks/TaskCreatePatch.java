package org.mcphackers.mcp.tasks;

import codechicken.diffpatch.cli.DiffOperation;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TaskCreatePatch extends Task {
	public TaskCreatePatch(int side, TaskInfo info) {
		super(side, info);
	}

	@Override
	public void doTask() throws Exception {
		Path srcPathUnpatched = Paths.get(chooseFromSide(MCPConfig.SRC + "minecraft_unpatched", MCPConfig.SRC + "minecraft_server_unpatched"));
		Path srcPathPatched = Paths.get(chooseFromSide(MCPConfig.CLIENT_SOURCES, MCPConfig.SERVER_SOURCES));
		Path patchesOut = Paths.get(chooseFromSide("patches/patches_client", "patches/patches_server"));
		if (Files.exists(srcPathUnpatched)) {
			if(Files.exists(srcPathPatched)) {
				createDiffOperation(srcPathUnpatched, srcPathPatched, patchesOut);
			}
			else {
				throw new Exception("Patched " + chooseFromSide("client", "server") + " sources cannot be found!");
			}
		} else {
			throw new Exception("Unpatched " + chooseFromSide("client", "server") + " sources cannot be found!");
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
			info.addInfo(logger.toString());
			throw new Exception("Patches could not be created!");
		}
	}
}
