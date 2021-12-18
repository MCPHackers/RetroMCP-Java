package org.mcphackers.mcp;

public enum EnumMode {

    help("Displays this menu"),
    decompile("Decompiles"),
    recompile("Recompiles"),
    reobfuscate("Reobfuscates classes"),
    updatemd5("Update current md5s"),
    updatemcp("Update"),
    setup("Setup"),
    exit("Exits the program");
	
	public String desc;
    
    private EnumMode(String desc) {
    	this.desc = desc;
    }
}
