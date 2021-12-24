package org.mcphackers.mcp;

import org.mcphackers.mcp.tasks.info.TaskInfo;
import org.mcphackers.mcp.tasks.info.TaskInfoDecompile;
import org.mcphackers.mcp.tasks.info.TaskInfoRecompile;
import org.mcphackers.mcp.tasks.info.TaskInfoReobfuscate;
import org.mcphackers.mcp.tasks.info.TaskInfoSetup;
import org.mcphackers.mcp.tasks.info.TaskInfoRun;
import org.mcphackers.mcp.tasks.info.TaskInfoUpdateMD5;

public enum EnumMode {

    help("Show this list", null),
    decompile("Start decompiling Minecraft", new TaskInfoDecompile()),
    recompile("Recompile Minecraft sources", new TaskInfoRecompile()),
    reobfuscate("Reobfuscate Minecraft classes", new TaskInfoReobfuscate()),
    updatemd5("Update md5 hash tables used for reobfuscation", new TaskInfoUpdateMD5()),
    updatemcp("Download an update if available", null),
    setup("Choose a version to setup", new TaskInfoSetup()),
    cleanup("Delete all source and class folders", null),
    startclient("Runs the client from compiled classes", new TaskInfoRun(0)),
    startserver("Runs the server from compiled classes", new TaskInfoRun(1)),
    exit("Exit the program", null);
	
	public String desc;
	public TaskInfo task;
    
    private EnumMode(String desc, TaskInfo task) {
    	this.desc = desc;
    	this.task = task;
    }
}
