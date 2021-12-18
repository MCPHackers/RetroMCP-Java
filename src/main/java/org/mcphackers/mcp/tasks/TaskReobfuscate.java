package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TaskReobfuscate implements Task {
    private final int side;

    private final Map<String, String> recompHashes = new HashMap<>();
    private final Map<String, String> originalHashes = new HashMap<>();

    public TaskReobfuscate(int side) {
        this.side = side;
    }

    @Override
    public void doTask() throws Exception {
        boolean clientCheck = checkBins(0);
        boolean serverCheck = checkBins(1);

        if (this.side == 0 && clientCheck) {
            if (Files.exists(Paths.get("reobf", "minecraft"))) {
                Utility.deleteDirectoryStream(Paths.get("reobf", "minecraft"));
            }

            // Create recompilation hashes and compare them to the original hashes
            new TaskUpdateMD5().doTask(true);
            // Recompiled hashes
            gatherMD5Hashes(true, this.side);
            // Original hashes
            gatherMD5Hashes(false, this.side);

            MCP.logger.info("> Compacting client bin directory");
            Utility.compress(Paths.get("bin", "minecraft"), Paths.get("temp", "client_reobf.jar"));
        }

        if (this.side == 1 && serverCheck) {
            if (Files.exists(Paths.get("reobf", "minecraft_server"))) {
                Utility.deleteDirectoryStream(Paths.get("reobf", "minecraft_server"));
            }

            // Create recompilation hashes and compare them to the original hashes
            new TaskUpdateMD5().doTask(true);
            // Recompiled hashes
            gatherMD5Hashes(true, this.side);
            // Original hashes
            gatherMD5Hashes(false, this.side);

            MCP.logger.info("> Compacting server bin directory");
            Utility.compress(Paths.get("bin", "minecraft_server"), Paths.get("temp", "server_reobf.jar"));
        }
    }

    @Override
    public ProgressInfo getProgress() {
        return new ProgressInfo("Decompiling...", 0, 1);
    }

    private void gatherMD5Hashes(boolean reobf, int side) {
        Path clientMD5 = reobf ? Paths.get("temp", "client_reobf.md5") : Paths.get("temp", "client.md5");
        Path serverMD5 = reobf ? Paths.get("temp", "server_reobf.md5") : Paths.get("temp", "server.md5");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(side == 0 ? clientMD5.toFile() : serverMD5.toFile()));
            String line = reader.readLine();
            while (line != null) {
                String[] tokens = line.split(" ");
                if (reobf) {
                    recompHashes.put(tokens[0], tokens[1]);
                } else {
                    originalHashes.put(tokens[0], tokens[1]);
                }

                // Read next line
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkBins(int side) {
        Path minecraft1 = Paths.get("bin", "minecraft", "net", "minecraft", "client", "Minecraft.class");
        Path minecraft2 = Paths.get("bin", "minecraft", "net", "minecraft", "src", "Minecraft.class");

        Path minecraftServer1 = Paths.get("bin", "minecraft_server", "net", "minecraft", "server", "MinecraftServer.class");
        Path minecraftServer2 = Paths.get("bin", "minecraft_server", "net", "minecraft", "src", "MinecraftServer.class");
        if (side == 0 && (Files.exists(minecraft1) || Files.exists(minecraft2))) {
            return true;
        } else if (side == 0) {
            return false;
        }

        if (side == 1 && (Files.exists(minecraftServer1) || Files.exists(minecraftServer2))) {
            return true;
        } else if (side == 1) {
            return false;
        }

        return false;
    }
}
