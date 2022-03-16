package org.mcphackers.mcp;

import java.util.HashMap;
import java.util.Map;

public enum EnumMode {

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
	private static final Map<String, String> paramDescs = new HashMap<>();
	
	EnumMode(String desc) {
		this.desc = desc;
	}
	
	EnumMode(String desc, String[] params) {
		this(desc);
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
