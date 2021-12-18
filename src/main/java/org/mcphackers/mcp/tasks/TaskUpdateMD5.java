package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TaskUpdateMD5 implements Task {
    @Override
    public void doTask() throws Exception {
        doTask(false);
    }

    public void doTask(boolean reobf) throws Exception {
        Path clientBinPath = Paths.get("bin", "minecraft");
        Path serverBinPath = Paths.get("bin", "minecraft_server");

        Path clientMD5 = reobf ? Paths.get("temp", "client_reobf.md5") : Paths.get("temp", "client.md5");
        Path serverMD5 = reobf ? Paths.get("temp", "server_reobf.md5") : Paths.get("temp", "server.md5");

        Files.deleteIfExists(clientMD5);
        Files.deleteIfExists(serverMD5);

        if (Files.exists(clientBinPath)) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(clientMD5.toFile()));

            Files.walkFileTree(clientBinPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        String md5_hash = Utility.getMD5OfFile(file.toFile());
                        String fileName = file.toString().replace("bin\\minecraft\\", "").replace("bin/minecraft/", "").replace(".class", "");
                        writer.append(fileName).append(" ").append(md5_hash).append("\n");
                        writer.flush();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            System.err.println("Client classes not found!");
        }

        if (Files.exists(serverBinPath)) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(serverMD5.toFile()));

            Files.walkFileTree(serverBinPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        String md5_hash = Utility.getMD5OfFile(file.toFile());
                        String fileName = file.toString().replace("bin\\minecraft_server\\", "").replace("bin/minecraft_server/", "").replace(".class", "");
                        writer.append(fileName).append(" ").append(md5_hash).append("\n");
                        writer.flush();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            System.err.println("Server classes not found!");
        }
    }

    @Override
    public ProgressInfo getProgress() {
        return new ProgressInfo("Updating MD5...", 0, 1);
    }
}
