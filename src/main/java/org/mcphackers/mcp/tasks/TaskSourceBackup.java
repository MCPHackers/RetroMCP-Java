package org.mcphackers.mcp.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;

public class TaskSourceBackup extends TaskStaged {

    public TaskSourceBackup(Side side, MCP instance) {
		super(side, instance);
	}

	public TaskSourceBackup(Side side, MCP instance, ProgressListener listener) {
		super(side, instance, listener);
	}

    @Override
	protected Stage[] setStages() {
        return new Stage[] {
            stage(getLocalizedStage("backupsrc"),
            () -> {
                packSourceIntoZip();
            })
        };
    }

    private void packSourceIntoZip(){
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        String filename = "src-backup-" + side.name + "-" + year + "-" + month + "-" + day + "@" + hour + "-" + minute + "-" + second;

        Path srcPath = MCPPaths.get(mcp, MCPPaths.SOURCE, side);
        try {
            //I went with a standard Paths.get here instead of MCPPaths.get because having separate client/server folders for these zips would be messy
            ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(Paths.get(MCPPaths.BACKUP, filename + ".zip")));

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
