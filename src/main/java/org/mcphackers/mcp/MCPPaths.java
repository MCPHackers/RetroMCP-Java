package org.mcphackers.mcp;

import java.nio.file.Path;

import org.mcphackers.mcp.tasks.Task.Side;

public class MCPPaths {
	
	//Directories
	public static final String JARS = 		"jars/";
	public static final String LIB = 		"libraries/";
	public static final String TEMP = 		"temp/";
	public static final String SRC = 		"src/";
	public static final String REOBF = 		"reobf/";
	public static final String CONF = 		"conf/";
	public static final String BUILD = 		"build/";
	public static final String WORKSPACE =  "workspace/";
	
	//Files and subdirectories
	public static final String JAR_ORIGINAL = 		 JARS + "minecraft_%s.jar";

	public static final String NATIVES = 			 LIB + "natives";

	public static final String TEMP_SIDE = 	 		 TEMP + "%s";
	public static final String TEMP_SRC = 	 		 TEMP + "%s/src";
	public static final String BIN =				 TEMP + "%s/bin";
	//public static final String COMPILED = 		 	 TEMP + "%s/compiled.jar";
	public static final String REMAPPED = 	 		 TEMP + "%s/remapped.jar";
	public static final String MD5 = 		 		 TEMP + "%s/original.md5";
	public static final String MD5_RO = 		 	 TEMP + "%s/modified.md5";
	public static final String REOBF_JAR = 	 		 TEMP + "%s/reobfuscated.jar";

	public static final String MAPPINGS = 			 CONF + "mappings.tiny";
	public static final String EXC = 		 		 CONF + "exceptions.exc";
	@Deprecated
	public static final String JAVADOCS = 		 	 CONF + "javadocs.txt";
	public static final String PATCHES = 	 		 CONF + "patches_%s";
	public static final String VERSION = 	 		 CONF + "version.json";

	public static final String SOURCE = 	 		 SRC + "minecraft_%s";

	public static final String REOBF_SIDE = 		 REOBF + "minecraft_%s";
	public static final String BUILD_ZIP = 	 		 BUILD + "minecraft_%s.zip";
	public static final String BUILD_JAR = 	 		 BUILD + "minecraft_%s.jar";

	public static final String UPDATE_JAR =		 	 "update.jar";
	
	public static Path get(MCP mcp, String path, Side side) {
		String newPath = path;
		if(side == Side.CLIENT) {
			newPath = path.replace("minecraft_%s", "minecraft");
		}
		return get(mcp, String.format(newPath, side.name));
	}

	public static Path get(MCP mcp, String path) {
		return mcp.getWorkingDir().resolve(path);
	}
}
