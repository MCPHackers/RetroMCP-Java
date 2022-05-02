package org.mcphackers.mcp.tools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mcphackers.mcp.MCP;
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
	public static final String DEOBF_OUT = 	 		 TEMP + "%s_deobf.jar";
	public static final String EXC_OUT = 	 		 TEMP + "%s_exc.jar";
	public static final String TEMP_EXC_OUT = 	 	 TEMP + "%s_exc_temp.jar";
	public static final String SIDE_SRC = 		 	 TEMP + "%s_src.zip";
	public static final String TEMP_SOURCES = 		 TEMP + "src/%s";
	public static final String MD5 = 		 		 TEMP + "%s.md5";
	public static final String MD5_RO = 		 	 TEMP + "%s_reobf.md5";
	public static final String REOBF_JAR = 	 		 TEMP + "%s_reobf.jar";
	public static final String MAPPINGS_RO =  		 TEMP + "%s_reobf.tiny";
	public static final String MAPPINGS_DO =  		 TEMP + "%s_deobf.tiny";
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
	
	private static final Map<String, Path> paths = new HashMap<>();
	private static Path cachedWorkingDir;
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
		if(mcp.getWorkingDir() != cachedWorkingDir) {
			cachedWorkingDir = mcp.getWorkingDir();
			paths.clear();
		}
		if(!paths.containsKey(path)) {
			Path p = cachedWorkingDir.resolve(Paths.get(path));
			paths.put(path, p);
			return p;
		}
		return paths.get(path);
	}
}
