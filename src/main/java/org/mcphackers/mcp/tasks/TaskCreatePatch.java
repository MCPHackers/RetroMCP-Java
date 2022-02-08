package org.mcphackers.mcp.tasks;

import codechicken.diffpatch.cli.DiffOperation;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TaskCreatePatch extends Task {
	public TaskCreatePatch(int side, TaskInfo info) {
		super(side, info);
	}

	@Override
	public void doTask() throws Exception {
		Path srcPath = Paths.get(chooseFromSide(MCPConfig.CLIENT_SOURCES, MCPConfig.SERVER_SOURCES));
		Path srcPathPatched = Paths.get(chooseFromSide(MCPConfig.SRC + "minecraft_patched", MCPConfig.SRC + "minecraft_server_patched"));
		Path patchesOut = Paths.get(chooseFromSide("patches/client", "patches/server"));
		if (Files.exists(srcPath)) {
			if(Files.exists(srcPathPatched)) {
				createDiffOperation(srcPath, srcPathPatched, patchesOut);
			}
			else {
				throw new Exception("Patched " + chooseFromSide("client", "server") + " sources cannot be found!");
			}
		} else {
			throw new Exception(chooseFromSide("Client", "Server") + " sources cannot be found!");
		}
	}

	public void createDiffOperation(Path aPath, Path bPath, Path outputPath) throws Exception {
		DiffOperation diffOperation = DiffOperation.builder()
				.aPath(aPath)
				.bPath(bPath)
				.outputPath(outputPath)
				.verbose(true)
				.summary(true).build();
		if(diffOperation.operate().exit != 0) {
			//throw new Exception("Patches could not be created!");
		}
	}
}
