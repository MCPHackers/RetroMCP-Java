package org.mcphackers.mcp;

public class MCPConfig {

	
	//Directories
	public static final String JARS = 	"jars/";
	public static final String LIB = 	"lib/";
	public static final String DEPS_C = LIB + "client/";
	public static final String DEPS_S = LIB + "server/";
	public static final String TEMP = 	"temp/";
	public static final String SRC = 	"src/";
	public static final String BIN = 	"bin/";
	public static final String REOBF = 	"reobf/";
	public static final String CONF = 	"conf/";
	public static final String BUILD = 	"build/";
	
	//Files and subdirectories
	public static final String CLIENT = 			 JARS + "minecraft.jar";
	public static final String SERVER = 			 JARS + "minecraft_server.jar";
	public static final String CLIENT_FIXED = 		 LIB + "minecraft.jar";
	public static final String LWJGL = 				 LIB + "lwjgl.jar";
	public static final String LWJGL_UTIL = 		 LIB + "lwjgl_util.jar";
	public static final String JINPUT = 	 		 LIB + "jinput.jar";
	public static final String NATIVES = 			 LIB + "natives";
	public static final String CLIENT_TINY_OUT = 	 TEMP + "client_deobf.jar";
	public static final String SERVER_TINY_OUT = 	 TEMP + "server_deobf.jar";
	public static final String CLIENT_EXC_OUT = 	 TEMP + "client_exc.jar";
	public static final String SERVER_EXC_OUT = 	 TEMP + "server_exc.jar";
	public static final String CLIENT_SRC = 		 TEMP + "client_src.zip";
	public static final String SERVER_SRC = 		 TEMP + "server_src.zip";
	public static final String CLIENT_TEMP_SOURCES = TEMP + "src/client";
	public static final String SERVER_TEMP_SOURCES = TEMP + "src/server";
	public static final String CLIENT_MD5 = 		 TEMP + "client.md5";
	public static final String SERVER_MD5 = 		 TEMP + "server.md5";
	public static final String CLIENT_MD5_RO = 		 TEMP + "client_reobf.md5";
	public static final String SERVER_MD5_RO = 		 TEMP + "server_reobf.md5";
	public static final String CLIENT_REOBF_JAR = 	 TEMP + "client_reobf.jar";
	public static final String SERVER_REOBF_JAR = 	 TEMP + "server_reobf.jar";
	public static final String CLIENT_MAPPINGS_RO =  TEMP + "client_reobf.tiny";
	public static final String SERVER_MAPPINGS_RO =  TEMP + "server_reobf.tiny";
	public static final String CLIENT_MAPPINGS_DO =  TEMP + "client_deobf.tiny";
	public static final String SERVER_MAPPINGS_DO =  TEMP + "server_deobf.tiny";
	public static final String CLIENT_SOURCES = 	 SRC + "minecraft";
	public static final String SERVER_SOURCES = 	 SRC + "minecraft_server";
	public static final String CLIENT_BIN = 		 BIN + "minecraft";
	public static final String SERVER_BIN = 		 BIN + "minecraft_server";
	public static final String CLIENT_REOBF = 		 REOBF + "minecraft";
	public static final String SERVER_REOBF = 		 REOBF + "minecraft_server";
	public static final String VERSION = 	 		 CONF + "version";
	public static final String CLIENT_MAPPINGS = 	 CONF + "client.tiny";
	public static final String SERVER_MAPPINGS = 	 CONF + "server.tiny";
	public static final String EXC_CLIENT = 		 CONF + "client.exc";
	public static final String EXC_SERVER = 		 CONF + "server.exc";
	public static final String CLIENT_PATCHES = 	 CONF + "patches_client";
	public static final String SERVER_PATCHES = 	 CONF + "patches_server";
	public static final String JAVADOC_CLIENT = 	 CLIENT_MAPPINGS;
	public static final String JAVADOC_SERVER = 	 SERVER_MAPPINGS;
	public static final String BUILD_ZIP_CLIENT = 	 BUILD + "minecraft.zip";
	public static final String BUILD_ZIP_SERVER = 	 BUILD + "minecraft_server.zip";
	public static final String BUILD_JAR_CLIENT = 	 BUILD + "minecraft.jar";
	public static final String BUILD_JAR_SERVER = 	 BUILD + "minecraft_server.jar";
	public static final String UPDATE_JAR 		= 	 "update.jar";
	
	public boolean debug;
	public boolean patch;
	public boolean srcCleanup;
	public String[] ignorePackages;
	public int onlySide;
	public int source;
	public int target;
	public String bootclasspath;
	public String indentionString;
	public boolean fullBuild;
	public boolean runBuild;
	public String setupVersion;
	public String[] runArgs;

	public MCPConfig() {
		resetConfig();
	}

	public void resetConfig() {
		debug = false;
		patch = true;
		srcCleanup = false;
		onlySide = -1;
		source = -1;
		target = -1;
		bootclasspath = null;
		ignorePackages = new String[]{"paulscode", "com/jcraft", "isom"};
		indentionString = "\t";
		fullBuild = false;
		runBuild = false;
		setupVersion = null;
		runArgs = null;
	}

	public void setParameter(String name, int value) {
		switch (name) {
			case "side":
				onlySide = value;
				break;
			case "source":
				source = value;
				break;
			case "target":
				target = value;
				break;
			default:
				// TODO: Cancel task
		}
	}

	public void setParameter(String name, String value) {
		switch (name) {
			case "ind":
			case "indention":
				indentionString = value;
				break;
			case "ignore":
				ignorePackages = new String[] {value};
				break;
			case "setupversion":
				setupVersion = value;
				break;
			case "bootclasspath":
				bootclasspath = value;
				break;
			default:
				// TODO: Cancel task
		}
	}

	public void setParameter(String name, String[] value) {
		switch (name) {
			case "ignore":
				ignorePackages = value;
				break;
			default:
				// TODO: Cancel task
		}
	}

	public void setParameter(String name, boolean value) {
		switch (name) {
			case "debug":
				debug = value;
				break;
			case "patch":
				patch = value;
				break;
			case "client":
				onlySide = value ? 0 : onlySide;
				break;
			case "server":
				onlySide = value ? 1 : onlySide;
				break;
			case "src":
				srcCleanup = value;
				break;
			case "fullbuild":
				fullBuild = value;
				break;
			case "runbuild":
				runBuild = value;
				break;
			default:
				// TODO: Cancel task
		}
	}

}
