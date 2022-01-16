package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.mcphackers.mcp.MCPConfig;
import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tools.ProgressInfo;
import org.mcphackers.mcp.tools.Util;

public class TaskBuild extends Task {

	private TaskReobfuscate reobfTask;

	private static final int REOBF = 1;
	private static final int BUILD = 2;
    private static final int STEPS = 2;

    public TaskBuild(int side, TaskInfo info) {
        super(side, info);
        reobfTask = new TaskReobfuscate(side, info);
    }

    @Override
    public void doTask() throws Exception {
    	Path originalJar =  Paths.get(chooseFromSide(MCPConfig.CLIENT, 			MCPConfig.SERVER));
        Path bin = 			Paths.get(chooseFromSide(MCPConfig.CLIENT_BIN, 		MCPConfig.SERVER_BIN));
    	Path reobfDir = 	Paths.get(chooseFromSide(MCPConfig.CLIENT_REOBF, 	MCPConfig.SERVER_REOBF));
    	Path buildJar = 	Paths.get(chooseFromSide(MCPConfig.BUILD_JAR_CLIENT, MCPConfig.BUILD_JAR_SERVER));
    	Path buildZip = 	Paths.get(chooseFromSide(MCPConfig.BUILD_ZIP_CLIENT, MCPConfig.BUILD_ZIP_SERVER));
        
		while(step < STEPS) {
		    step();
		    switch (step) {
		    case REOBF:
			    this.reobfTask.doTask();
		    	break;
		    case BUILD:
		    	Util.createDirectories(Paths.get(MCPConfig.BUILD));
		    	if(MCPConfig.fullBuild) {
			    	Files.deleteIfExists(buildJar);
		    		Files.copy(originalJar, buildJar);
		    		List<Path> reobfClasses = Util.listDirectory(reobfDir, path -> !Files.isDirectory(path));
		    		Util.packFilesToZip(buildJar, reobfClasses, reobfDir);
			    	List<Path> assets = Util.listDirectory(bin, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class"));
		    		Util.packFilesToZip(buildJar, assets, bin);
		    		Util.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.DSA");
		    		Util.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.SF");
		    	}
		    	else {
			    	Files.deleteIfExists(buildZip);
		    		Util.compress(reobfDir, buildZip);
		    		List<Path> assets = Util.listDirectory(bin, path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class"));
		    		Util.packFilesToZip(buildZip, assets, bin);
		    	}
		    	break;
			}
		}
    }

    public ProgressInfo getProgress() {
    	int total = 100;
    	int current = 0;
	    switch (step) {
	    case REOBF: {
        	current = 1;
        	ProgressInfo info = reobfTask.getProgress();
        	int percent = (int) ((double)info.getCurrent() / info.getTotal() * 49);
            return new ProgressInfo(info.getMessage(), current + percent, total); }
	    case BUILD:
        	current = 52;
            return new ProgressInfo("Building...", current, total);
	    default:
	    	return super.getProgress();
        }
    }
}
