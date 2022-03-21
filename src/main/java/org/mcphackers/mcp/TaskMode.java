package org.mcphackers.mcp;

import java.util.HashMap;
import java.util.Map;

public enum TaskMode {

	//TODO
	help("Displays command usage"),
	decompile("Start decompiling Minecraft", new String[] {"debug", "ignore", "indention", "patch", "side", "client", "server"}),
	recompile("Recompile Minecraft sources", new String[] {"debug", "side", "client", "server"}),
	reobfuscate("Reobfuscate Minecraft classes", new String[] {"debug", "side", "client", "server"}),
	updatemd5("Update md5 hash tables used for reobfuscation", new String[] {"debug", "side", "client", "server"}),
	updatemcp("Download an update if available"),
	setup("Choose a version to setup", new String[] {"debug"}),
	cleanup("Delete all source and class folders", new String[] {"debug", "src"}),
	startclient("Runs the client from compiled classes", new String[] {"runbuild"}),
	startserver("Runs the server from compiled classes", new String[] {"runbuild"}),
	build("Builds the final jar or zip", new String[] {"debug", "fullbuild", "side", "client", "server"}),
	createpatch("Creates patch", new String[]{}),
	exit("Exit the program", null);
	
	public final String desc;
	public String[] params = new String[] {};
	private static final Map<String, TaskParameter> paramDescs = new HashMap<>();
	
	TaskMode(String desc) {
		this.desc = desc;
	}
	
	TaskMode(String desc, String[] params) {
		this(desc);
		this.params = params;
		
	}
	
	public static String getParamDesc(String param) {
		if(paramDescs.containsKey(param)) {
			return paramDescs.get(param).name;
		}
		return "No description provided";
	}
	
	//TODO
	static {
		paramDescs.put("indention", TaskParameter.INDENTION_STRING);
		paramDescs.put("ignore", TaskParameter.IGNORED_PACKAGES);
		paramDescs.put("debug", TaskParameter.DEBUG);
		paramDescs.put("patch", TaskParameter.PATCHES);
		paramDescs.put("side", TaskParameter.SIDE);
		paramDescs.put("client", TaskParameter.SIDE);
		paramDescs.put("server", TaskParameter.SIDE);
		paramDescs.put("src", null);
		paramDescs.put("fullbuild", TaskParameter.FULL_BUILD);
		paramDescs.put("runbuild", TaskParameter.RUN_BUILD);
	}
}
