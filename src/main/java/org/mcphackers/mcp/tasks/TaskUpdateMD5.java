package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tools.ProgressInfo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TaskUpdateMD5 implements Task {
    @Override
    public void doTask() throws Exception {
        TaskRecompile recompileTask = new TaskRecompile();
        recompileTask.doTask();

        Path clientBinPath = Paths.get("bin", "minecraft");
        Path serverBinPath = Paths.get("bin", "minecraft_server");

        if (Files.exists(clientBinPath)) {
            Iterable<Path> classes = Files.walk(clientBinPath).filter(path -> !Files.isDirectory(path)).filter(path -> path.endsWith(".class")).collect(Collectors.toList());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get("temp", "client.md5").toFile()))) {
                for (Path path : classes) {
                    byte[] bytes = Files.readAllBytes(path);
                    byte[] md5_hash = MessageDigest.getInstance("MD5").digest(bytes);
                    writer.write(path.toString().replace("src/minecraft/", "") + " " + Arrays.toString(md5_hash));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.err.println("Client classes not found!");
        }

        if (Files.exists(serverBinPath)) {
            Iterable<Path> classes = Files.walk(serverBinPath).filter(path -> !Files.isDirectory(path)).filter(path -> path.endsWith(".class")).collect(Collectors.toList());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get("temp", "server.md5").toFile()))) {
                for (Path path : classes) {
                    byte[] bytes = Files.readAllBytes(path);
                    byte[] md5_hash = MessageDigest.getInstance("MD5").digest(bytes);
                    writer.write(path.toString().replace("src/minecraft_server/", "") + " " + Arrays.toString(md5_hash));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.err.println("Server classes not found!");
        }
    }

    @Override
    public ProgressInfo getProgress() {
        return new ProgressInfo("Updating MD5...", 0, 1);
    }
}
