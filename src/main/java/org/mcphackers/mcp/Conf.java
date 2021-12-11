package org.mcphackers.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Conf {

    public static final Path JARS = Paths.get("jars/");
    public static final Path CLIENT = Paths.get("jars/bin/minecraft.jar");
    public static final Path SERVER = Paths.get("jars/minecraft_server.jar");
    public static final Path CLIENT_RG_OUT = Paths.get("temp/minecraft_rg.jar");
    public static final Path SERVER_RG_OUT = Paths.get("temp/minecraft_server_rg.jar");
    public static final Path CLIENT_FF_OUT = Paths.get("temp/minecraft.jar");
    public static final Path SERVER_FF_OUT = Paths.get("temp/minecraft_server.jar");
    public static final Path CFG_RG = Paths.get("temp/retroguard.cfg");
    public static final Path CFG_RG_RO = Paths.get("temp/retroguard_ro.cfg");
    public static boolean debug;

    static {
        resetConfig();
    }

    public static void resetConfig() {
        debug = false;
    }

}
