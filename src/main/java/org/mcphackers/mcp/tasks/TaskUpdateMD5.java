package org.mcphackers.mcp.tasks;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Utility;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class TaskUpdateMD5 extends Task {
	private int total;
	private int progress;
	
	
    
    public TaskUpdateMD5(int side, TaskInfo info) {
        super(side, info);
        this.total = 1;
        this.progress = 0;
	}

	@Override
    public void doTask() throws Exception {
        updateMD5(false);
    }

    public void updateMD5(boolean reobf) throws Exception {
        Path binPath = side == 1 ? Utility.getPath(MCPConfig.SERVER_BIN) : Utility.getPath(MCPConfig.CLIENT_BIN);
        Path md5 = side == 1 ? (reobf ? Utility.getPath(MCPConfig.SERVER_MD5_RO) : Utility.getPath(MCPConfig.SERVER_MD5)) : (reobf ? Utility.getPath(MCPConfig.CLIENT_MD5_RO) : Utility.getPath(MCPConfig.CLIENT_MD5));
        step();
        if(!reobf) new TaskRecompile(side, info).doTask();
        step();
        if (Files.exists(binPath)) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(md5.toFile()));
            this.total = (int)Files.walk(binPath)
                    .parallel()
                    .filter(p -> !p.toFile().isDirectory())
                    .count();
            Files.walkFileTree(binPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        String md5_hash = Utility.getMD5OfFile(file.toFile());
                        String fileName = ((side == 1 ? Utility.getPath(MCPConfig.SERVER_BIN) : Utility.getPath(MCPConfig.CLIENT_BIN)).relativize(file).toString().replace(".class", ""));
                        writer.append(fileName).append(" ").append(md5_hash).append("\n");
                        progress++;
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
        switch(step) {
        case 1:
        	return new ProgressInfo("Recompiling...", 0, 1);
        case 2:
        	return new ProgressInfo("Updating MD5...", progress, total);
        }
        return super.getProgress();
    }
}
