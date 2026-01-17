package org.mcphackers.mcp;

import java.nio.file.Path;

import org.mcphackers.mcp.tasks.Task.Side;

public class MCPPaths {

	//Directories
	public static final String JARS = "jars/";
	public static final String LIB = "libraries/";
	public static final String CONF = "conf/";
	public static final String BUILD = "build/";
	public static final String PROJECT = "minecraft_%s/";

	//Files and subdirectories
	public static final String JAR_ORIGINAL = JARS + "minecraft_%s.jar";

	public static final String NATIVES = LIB + "natives";

	public static final String BUILD_ZIP = BUILD + "minecraft_%s.zip";
	public static final String BUILD_JAR = BUILD + "minecraft_%s.jar";

	public static final String SOURCE_UNPATCHED = PROJECT + "src_original";
	public static final String SOURCE = PROJECT + "src";
	public static final String BIN = PROJECT + "bin";
	public static final String MD5_DIR = PROJECT + "md5";
	public static final String MD5 = PROJECT + "md5/original.md5";
	public static final String MD5_RO = PROJECT + "md5/modified.md5";
	public static final String JARS_DIR = PROJECT + "jars";
	public static final String REMAPPED = PROJECT + "jars/deobfuscated.jar";
	public static final String REOBF_JAR = PROJECT + "jars/reobfuscated.jar";
	public static final String SOURCE_JAR = PROJECT + "jars/deobfuscated-source.jar";
	public static final String REOBF_SIDE = PROJECT + "reobf";
	public static final String GAMEDIR = PROJECT + "game/";

	public static final String MAPPINGS = CONF + "mappings.tiny";
	public static final String EXC = CONF + "exceptions.exc";
	public static final String ACCESS = CONF + "%s.access";
	public static final String CONF_PATCHES = CONF + "%s.patch";
	public static final String PATCHES = "patches/%s.patch";
	public static final String VERSION = CONF + "version.json";


	public static final String UPDATE_JAR = "update.jar";

	public static Path get(MCP mcp, String path, Side side) {
		String newPath = path;
		if (side == Side.CLIENT) {
			newPath = path.replace("minecraft_%s", "minecraft");
		}
		return get(mcp, String.format(newPath, side.name));
	}

	public static Path get(MCP mcp, String path) {
		return mcp.getWorkingDir().resolve(path);
	}
}
