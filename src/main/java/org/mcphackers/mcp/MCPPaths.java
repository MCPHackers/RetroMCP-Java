package org.mcphackers.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.mcphackers.mcp.tasks.Task.Side;

public class MCPPaths {

	
	//Directories
	public static final String JARS = 	"jars/";
	public static final String LIB = 	"lib/";
	public static final String TEMP = 	"temp/";
	public static final String SRC = 	"src/";
	public static final String BIN = 	"bin/";
	public static final String REOBF = 	"reobf/";
	public static final String CONF = 	"conf/";
	public static final String BUILD = 	"build/";
	
	//Files and subdirectories
	public static final String JAR_ORIGINAL = 		 JARS + "minecraft_%s.jar";
	public static final String LIBS =			 	 LIB + "%s/";
	public static final String LIBS_CLIENT =		 String.format(LIBS, Side.CLIENT.name.toLowerCase());
	public static final String CLIENT_FIXED = 		 LIBS_CLIENT + "minecraft.jar";
	public static final String LWJGL = 				 LIBS_CLIENT + "lwjgl.jar";
	public static final String LWJGL_UTIL = 		 LIBS_CLIENT + "lwjgl_util.jar";
	public static final String JINPUT = 	 		 LIBS_CLIENT + "jinput.jar";
	public static final String NATIVES = 			 LIBS_CLIENT + "natives";
	public static final String TEMP_SIDE = 	 		 TEMP + "%s";
	public static final String DEOBF_OUT = 	 		 TEMP + "%s/deobf.jar";
	public static final String EXC_OUT = 	 		 TEMP + "%s/exc.jar";
	public static final String TEMP_EXC_OUT = 	 	 TEMP + "%s/exc_temp.jar";
	public static final String SIDE_SRC = 		 	 TEMP + "%s/src.zip";
	public static final String TEMP_SOURCES = 		 TEMP + "%s/src";
	public static final String MD5 = 		 		 TEMP + "%s/original.md5";
	public static final String MD5_RO = 		 	 TEMP + "%s/modified.md5";
	public static final String REOBF_JAR = 	 		 TEMP + "%s/reobf.jar";
	public static final String MAPPINGS_RO =  		 TEMP + "%s/reobf.tiny";
	public static final String MAPPINGS_DO =  		 TEMP + "%s/deobf.tiny";
	public static final String SOURCES = 	 		 SRC + "minecraft_%s";
	public static final String BIN_SIDE = 		 	 BIN + "minecraft_%s";
	public static final String REOBF_SIDE = 		 REOBF + "minecraft_%s";
	public static final String MAPPINGS = 			 CONF + "%s.tiny";
	public static final String EXC = 		 		 CONF + "%s.exc";
	public static final String PATCHES = 	 		 CONF + "patches_%s";
	public static final String BUILD_ZIP = 	 		 BUILD + "minecraft_%s.zip";
	public static final String BUILD_JAR = 	 		 BUILD + "minecraft_%s.jar";
	public static final String VERSION = 	 		 CONF + "version";
	public static final String UPDATE_JAR 		= 	 "update.jar";
	
	private static final Set<String> stripClient = new HashSet<String>() {{
		add(JAR_ORIGINAL);
		add(SOURCES);
		add(BIN_SIDE);
		add(REOBF_SIDE);
		add(BUILD_ZIP);
		add(BUILD_JAR);
	}};
	
	public static Path get(MCP mcp, String path, Side side) {
		if(side == Side.CLIENT && stripClient.contains(path)) {
			return get(mcp, path.replace("_%s", ""));
		}
		return get(mcp, String.format(path, side.name.toLowerCase()));
	}

	public static Path get(MCP mcp, String path) {
		return mcp.getWorkingDir().resolve(Paths.get(path));
	}
}
