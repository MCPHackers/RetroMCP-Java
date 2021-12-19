package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.Conf;
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
        Path binPath = side == 1 ? Utility.getPath(Conf.SERVER_BIN) : Utility.getPath(Conf.CLIENT_BIN);
        Path md5 = side == 1 ? (reobf ? Utility.getPath(Conf.SERVER_MD5_RO) : Utility.getPath(Conf.SERVER_MD5)) : (reobf ? Utility.getPath(Conf.CLIENT_MD5_RO) : Utility.getPath(Conf.CLIENT_MD5));

        if (Files.exists(binPath)) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(md5.toFile()));
            
            //TODO: Find out why binPath of Client gets locked in here
            Files.walkFileTree(binPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        String md5_hash = Utility.getMD5OfFile(file.toFile());
                        String fileName = file.toString().replace((side == 1 ? Conf.SERVER_BIN : Conf.CLIENT_BIN).replace("/", File.separator), "").replace(".class", "");
                        writer.append(fileName).append(" ").append(md5_hash).append("\n");
                        writer.flush();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            writer.close();
        } else {
        	throw new IOException((side == 1 ? "Server" : "Client") + " classes not found!");
        }
    }

    @Override
    public ProgressInfo getProgress() {
        return new ProgressInfo("Updating MD5...", 0, 1);
    }
}
