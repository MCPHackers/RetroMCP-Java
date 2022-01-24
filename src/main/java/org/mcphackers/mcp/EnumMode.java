package org.mcphackers.mcp;

import java.util.HashMap;
import java.util.Map;

import org.mcphackers.mcp.tasks.info.*;

public enum EnumMode {

    help("Displays command usage", null),
    decompile("Start decompiling Minecraft", new TaskInfoDecompile(), new String[] {"debug", "ignore", "indention", "patch", "side", "client", "server"}),
    recompile("Recompile Minecraft sources", new TaskInfoRecompile(), new String[] {"debug", "side", "client", "server"}),
    reobfuscate("Reobfuscate Minecraft classes", new TaskInfoReobfuscate(), new String[] {"debug", "side", "client", "server"}),
    updatemd5("Update md5 hash tables used for reobfuscation", new TaskInfoUpdateMD5(), new String[] {"debug", "side", "client", "server"}),
    updatemcp("Download an update if available", new TaskInfoDownloadUpdate()),
    setup("Choose a version to setup", new TaskInfoSetup(), new String[] {"debug"}),
    cleanup("Delete all source and class folders", new TaskInfoCleanup(), new String[] {"debug", "src"}),
    startclient("Runs the client from compiled classes", new TaskInfoRun(0), new String[] {"runbuild"}),
    startserver("Runs the server from compiled classes", new TaskInfoRun(1), new String[] {"runbuild"}),
    build("Builds the final jar or zip", new TaskInfoBuild(), new String[] {"debug", "fullbuild", "side", "client", "server"}),
    exit("Exit the program", null);
	
	public final String desc;
	public final TaskInfo task;
	public String[] params = new String[] {};
	private static final Map<String, String> paramDescs = new HashMap<String, String>();
    
    private EnumMode(String desc, TaskInfo task) {
    	this.desc = desc;
    	this.task = task;
    }
    
    private EnumMode(String desc, TaskInfo task, String[] params) {
    	this(desc, task);
    	this.params = params;
    	
    }
    
    public static String getParamDesc(String param) {
    	if(paramDescs.containsKey(param)) {
    		return paramDescs.get(param);
    	}
    	return "No description provided";
    }
    
    static {
    	paramDescs.put("indention", "Indention character used for sources");
    	paramDescs.put("ignore", "List of packages to ignore");
    	paramDescs.put("debug", "Show exception stack trace");
    	paramDescs.put("patch", "Apply patches");
    	paramDescs.put("side", "Performs operation only for specified side");
    	paramDescs.put("client", "Performs operation only for client");
    	paramDescs.put("server", "Performs operation only for server");
    	paramDescs.put("src", "Only clear sources and classes folders");
    	paramDescs.put("fullbuild", "Builds a runnable jar");
    	paramDescs.put("runbuild", "Runs the built jar");
    }
}
