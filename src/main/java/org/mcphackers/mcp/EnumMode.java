package org.mcphackers.mcp;

public enum EnumMode {

    help("Show this list", false),
    decompile("Start decompiling Minecraft", true),
    recompile("Recompile Minecraft sources", true),
    reobfuscate("Reobfuscate Minecraft classes", true),
    updatemd5("Update md5 hash tables used for reobfuscation", true),
    updatemcp("Download an update if available", true),
    setup("Choose a version to setup", true),
    cleanup("Delete all source and class folders", true),
    exit("Exit the program", false);
	
	public String desc;
	public boolean isTask;
    
    private EnumMode(String desc, boolean task) {
    	this.desc = desc;
    	this.isTask = task;
    }
}
