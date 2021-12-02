package org.mcphackers.mcp;

public class MCPConfig {

	public static boolean debug;
	
	public static void resetConfig()
	{
		debug = false;
	}
	
	static
	{
	    resetConfig();
	}
	
}
