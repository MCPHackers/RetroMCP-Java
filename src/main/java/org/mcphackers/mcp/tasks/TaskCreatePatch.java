package org.mcphackers.mcp.tasks;

import codechicken.diffpatch.cli.DiffOperation;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TaskCreatePatch extends Task {
    public TaskCreatePatch(TaskInfo info) {
        super(-1, info);
    }

    @Override
    public void doTask() throws Exception {
        if (Files.exists(Paths.get(MCPConfig.CLIENT_SOURCES)) && Files.exists(Paths.get(MCPConfig.SRC + "minecraft_patched"))) {
            createDiffOperation(Paths.get(MCPConfig.CLIENT_SOURCES), Paths.get(MCPConfig.SRC + "minecraft_patched"), Paths.get("patches_client"));
        } else {
            MCP.logger.log("Client (or patched) sources cannot be found!");
            return;
        }

        if (Files.exists(Paths.get(MCPConfig.SERVER_SOURCES)) && Files.exists(Paths.get(MCPConfig.SRC + "minecraft_server_patched"))) {
            createDiffOperation(Paths.get(MCPConfig.SERVER_SOURCES), Paths.get(MCPConfig.SRC + "minecraft_server_patched"), Paths.get("patches_server"));
        } else {
            MCP.logger.log("Server (or patched) sources cannot be found!");
            return;
        }
    }

    public void createDiffOperation(Path aPath, Path bPath, Path outputPath) throws IOException {
        DiffOperation diffOperation = DiffOperation.builder()
                .aPath(aPath)
                .bPath(bPath)
                .outputPath(outputPath)
                .verbose(true)
                .summary(true).build();
        diffOperation.operate();
    }
}
