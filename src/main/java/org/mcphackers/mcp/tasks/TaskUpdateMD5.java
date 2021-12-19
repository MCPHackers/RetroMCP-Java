package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TaskUpdateMD5 implements Task {
    private final int side;
    
    public TaskUpdateMD5(int side) {
        this.side = side;
	}

	@Override
    public void doTask() throws Exception {
        doTask(false);
    }

    public void doTask(boolean reobf) throws Exception {
        Path binPath = side == 1 ? Paths.get("bin", "minecraft_server") : Paths.get("bin", "minecraft");
        Path md5 = side == 1 ? (reobf ? Paths.get("temp", "server_reobf.md5") : Paths.get("temp", "server.md5")) : (reobf ? Paths.get("temp", "client_reobf.md5") : Paths.get("temp", "client.md5"));

        if (Files.exists(binPath)) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(md5.toFile()));

            Files.walkFileTree(binPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        String md5_hash = Utility.getMD5OfFile(file.toFile());
                        String fileName = file.toString().replace("bin/minecraft/".replace("/", File.separator), "").replace(".class", "");
                        writer.append(fileName).append(" ").append(md5_hash).append("\n");
                        writer.flush();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
        	throw new IOException((side == 1 ? "Server" : "Client") + " classes not found!");
        }
    }

    @Override
    public ProgressInfo getProgress() {
        return new ProgressInfo("Updating MD5...", 0, 1);
    }
}
