package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

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
    	Path originalJar =  Util.getPath(side == 1 ? MCPConfig.SERVER 		: MCPConfig.CLIENT);
        Path bin = 			Util.getPath(side == 1 ? MCPConfig.SERVER_BIN 	: MCPConfig.CLIENT_BIN);
    	Path reobfDir = 	Util.getPath(side == 1 ? MCPConfig.SERVER_REOBF : MCPConfig.CLIENT_REOBF);
    	Path buildJar = 	Util.getPath(side == 1 ? MCPConfig.BUILD_JAR_SERVER : MCPConfig.BUILD_JAR_CLIENT);
    	Path buildZip = 	Util.getPath(side == 1 ? MCPConfig.BUILD_ZIP_SERVER : MCPConfig.BUILD_ZIP_CLIENT);
        
		while(step < STEPS) {
		    step();
		    switch (step) {
		    case REOBF:
			    this.reobfTask.doTask();
		    	break;
		    case BUILD:
		    	if(!Files.exists(Paths.get("build"))) {
		    		Files.createDirectory(Paths.get("build"));
		    	}
		    	if(MCPConfig.fullBuild) {
			    	Files.deleteIfExists(buildJar);
		    		Files.copy(originalJar, buildJar);
			    	Iterable<Path> reobfClasses = Files.walk(reobfDir).filter(path -> !Files.isDirectory(path)).collect(Collectors.toList());
		    		Util.packFilesToZip(buildJar, reobfClasses, reobfDir);
			    	Iterable<Path> assets = Files.walk(bin).filter(path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class")).collect(Collectors.toList());
		    		Util.packFilesToZip(buildJar, assets, bin);
		    		Util.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.DSA");
		    		Util.deleteFileInAZip(buildJar, "/META-INF/MOJANG_C.SF");
		    	}
		    	else {
			    	Files.deleteIfExists(buildZip);
		    		Util.compress(reobfDir, buildZip);
			    	Iterable<Path> assets = Files.walk(bin).filter(path -> !Files.isDirectory(path) && !path.getFileName().toString().endsWith(".class")).collect(Collectors.toList());
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
        	int percent = (int) (info.getCurrent() * 100 / info.getTotal() * 0.49D);
            return new ProgressInfo(info.getMessage(), current + percent, total); }
	    case BUILD:
        	current = 52;
            return new ProgressInfo("Building...", current, total);
	    default:
	    	return super.getProgress();
        }
    }
}
