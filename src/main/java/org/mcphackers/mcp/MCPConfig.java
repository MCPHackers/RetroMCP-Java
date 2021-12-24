package org.mcphackers.mcp;

public class MCPConfig {
	
	public static final String CLIENT = "jars/bin/minecraft.jar";
	public static final String SERVER = "jars/minecraft_server.jar";
	public static final String CLIENT_TINY_OUT = "temp/minecraft_remapped.jar";
	public static final String SERVER_TINY_OUT = "temp/minecraft_server_remapped.jar";
	public static final String CLIENT_EXC_OUT = "temp/minecraft_exc.jar";
	public static final String SERVER_EXC_OUT = "temp/minecraft_server_exc.jar";
	public static final String CLIENT_SRC = "temp/minecraft_src.zip";
	public static final String SERVER_SRC = "temp/minecraft_server_src.zip";
	public static final String CLIENT_TEMP_SOURCES = "temp/src/minecraft";
	public static final String SERVER_TEMP_SOURCES = "temp/src/minecraft_server";
	public static final String CLIENT_SOURCES = "src/minecraft";
	public static final String SERVER_SOURCES = "src/minecraft_server";
	public static final String CLIENT_BIN = "bin/minecraft";
	public static final String SERVER_BIN = "bin/minecraft_server";
	public static final String CLIENT_MAPPINGS = "conf/client.tiny";
	public static final String SERVER_MAPPINGS = "conf/server.tiny";
	public static final String CLIENT_MAPPINGS_RO = "temp/client_reobf.tiny";
	public static final String SERVER_MAPPINGS_RO = "temp/server_reobf.tiny";
	public static final String EXC_CLIENT = "conf/client.exc";
	public static final String EXC_SERVER = "conf/server.exc";
	public static final String CLIENT_PATCHES = "conf/patches_client";
	public static final String SERVER_PATCHES = "conf/patches_server";
	public static final String CLIENT_MD5 = "temp/client.md5";
	public static final String SERVER_MD5 = "temp/server.md5";
	public static final String CLIENT_MD5_RO = "temp/client_reobf.md5";
	public static final String SERVER_MD5_RO = "temp/server_reobf.md5";
	public static final String CLIENT_REOBF = "reobf/minecraft";
	public static final String SERVER_REOBF = "reobf/minecraft_server";
	public static final String CLIENT_REOBF_JAR = "temp/client_reobf.jar";
	public static final String SERVER_REOBF_JAR = "temp/server_reobf.jar";
	public static final String LWJGL = "jars/bin/lwjgl.jar";
	public static final String LWJGL_UTIL = "jars/bin/lwjgl_util.jar";
	public static final String JINPUT = "jars/bin/jinput.jar";
	public static final String NATIVES = "jars/bin/natives";
	
	public static boolean debug;
	public static boolean patch;
	public static String[] ignorePackages;
	public static int onlySide;
	public static String indentionString;

    static {
        resetConfig();
    }

    public static void resetConfig() {
        debug = false;
        patch = true;
    	onlySide = -1;
    	ignorePackages = new String[]{"paulscode", "com/jcraft", "isom"};
    	indentionString = "\t";
    }

    public static void setParameter(String name, int value) {
        switch (name) {
        	case "side":
        		onlySide = value;
        		break;
        }
    }

    public static void setParameter(String name, String value) {
        switch (name) {
	        case "ind":
	        case "indention":
	        	indentionString = value;
	            break;
        }
    }

    public static void setParameter(String name, String[] value) {
        switch (name) {
	        case "ignore":
	            ignorePackages = value;
	            break;
        }
    }

    public static void setParameter(String name, boolean value) {
        switch (name) {
            case "debug":
                debug = value;
                break;
            case "patch":
                patch = value;
                break;
        }
    }

}
