package org.mcphackers.mcp;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Conf {

    public static final Path JARS = Paths.get("jars/");
    public static final Path CLIENT = Paths.get("jars/bin/minecraft.jar");
    public static final Path SERVER = Paths.get("jars/minecraft_server.jar");
    public static final Path CLIENT_CLASSES = Paths.get("temp/cls/minecraft");
    public static final Path SERVER_CLASSES = Paths.get("temp/cls/minecraft_server");
    public static final Path CLIENT_SOURCES = Paths.get("temp/src/minecraft");
    public static final Path SERVER_SOURCES = Paths.get("temp/src/minecraft_server");
    public static boolean debug;

    static {
        resetConfig();
    }

    public static void resetConfig() {
        debug = false;
    }

}
