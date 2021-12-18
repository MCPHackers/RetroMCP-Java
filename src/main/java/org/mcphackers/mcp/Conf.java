package org.mcphackers.mcp;

public class Conf {

    public static final String JARS = "jars";
    public static final String CLIENT = "jars/bin/minecraft.jar";
    public static final String SERVER = "jars/minecraft_server.jar";
    public static final String CLIENT_RG_OUT = "temp/minecraft_rg.jar";
    public static final String SERVER_RG_OUT = "temp/minecraft_server_rg.jar";
    public static final String CLIENT_EXC_OUT = "temp/minecraft_exc.jar";
    public static final String SERVER_EXC_OUT = "temp/minecraft_server_exc.jar";
    public static final String CLIENT_FF_OUT = "temp/minecraft.jar";
    public static final String SERVER_FF_OUT = "temp/minecraft_server.jar";
    public static final String CFG_RG = "temp/retroguard.cfg";
    public static final String CFG_RG_RO = "temp/retroguard_ro.cfg";

    public static final String CLIENT_MAPPINGS = "conf/client.tiny";
    public static final String SERVER_MAPPINGS = "conf/server.tiny";

    public static boolean debug;

    static {
        resetConfig();
    }

    public static void resetConfig() {
        debug = false;
    }

}
