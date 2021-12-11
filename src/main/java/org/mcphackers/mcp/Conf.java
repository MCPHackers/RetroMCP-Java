package org.mcphackers.mcp;

import java.io.File;

public class Conf {

	public static boolean debug;
	
	public static final String JARS				 = path("jars/");
	public static final String CLIENT 	 		 = path("jars/bin/minecraft.jar");
	public static final String SERVER			 = path("jars/minecraft_server.jar");
	public static final String CLIENT_RG_OUT 	 = path("temp/minecraft_rg.jar");
	public static final String SERVER_RG_OUT 	 = path("temp/minecraft_server_rg.jar");
	public static final String CLIENT_FF_OUT 	 = path("temp/minecraft.jar");
	public static final String SERVER_FF_OUT 	 = path("temp/minecraft_server.jar");
	public static final String CFG_RG 	 	 	 = path("temp/retroguard.cfg");
	public static final String CFG_RG_RO 	 	 = path("temp/retroguard_ro.cfg");
	
	public static void resetConfig()
	{
		debug = false;
	}
	
	private static String path(String path)
	{
		return path.replace("/", File.separator);
	}
	
	static
	{
	    resetConfig();
	}
	
}
