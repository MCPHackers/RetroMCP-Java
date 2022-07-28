package org.mcphackers.mcp.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;

public class TaskSourceBackup extends Task {
	
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    public TaskSourceBackup(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskSourceBackup(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}
	
	@Override
	public void doTask() throws Exception {
        packSourceIntoZip();
	}

    private void packSourceIntoZip() {

        String filename = "src-backup-" + side.name + "-" + DATE_FORMATTER.format(Instant.now());
        Path backupPath = MCPPaths.get(mcp, filename + ".zip");
        for(int i = 0; Files.exists(backupPath); i++) {
        	backupPath = MCPPaths.get(mcp, filename + (i == 0 ? "" : "-" + i) + ".zip");
        }

        Path srcPath = MCPPaths.get(mcp, MCPPaths.SOURCE, side);
        try {
            ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(backupPath));

            List<String> srcFiles = new ArrayList<>();
            Files.walk(srcPath).forEach(file -> {
                if(file.getFileName().toString().endsWith(".java")){
                    srcFiles.add(file.toString());
                }
            });
            long nFiles = srcFiles.size();

            int i = 0;
            for (String path : srcFiles) {
                if(path.endsWith(".java")) {
                    try {
                        zip.putNextEntry(new ZipEntry(path));
                        zip.write(Files.readAllBytes(Paths.get(path)));
                        zip.closeEntry();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                setProgress(MCP.TRANSLATOR.translateKey("task.stage.backupsrc") + " " + path, (int)((i / (float)nFiles) * 100));
                i++;
            }
            zip.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
