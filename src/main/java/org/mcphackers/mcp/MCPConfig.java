package org.mcphackers.mcp;

public class MCPConfig {

    public static final String CLIENT = "jars/bin/minecraft.jar";
    public static final String SERVER = "jars/minecraft_server.jar";
    public static final String CLIENT_RG_OUT = "temp/minecraft_rg.jar";
    public static final String SERVER_RG_OUT = "temp/minecraft_server_rg.jar";
    public static final String CLIENT_EXC_OUT = "temp/minecraft_exc.jar";
    public static final String SERVER_EXC_OUT = "temp/minecraft_server_exc.jar";
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

}
