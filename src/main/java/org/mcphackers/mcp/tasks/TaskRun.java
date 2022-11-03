package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.OS;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.DownloadData;
import org.mcphackers.mcp.tools.versions.json.Version;
import org.mcphackers.mcp.tools.versions.json.Version.Arguments;

public class TaskRun extends Task {

	public static final List<String> SERVER_MAIN = Arrays.asList("net.minecraft.server.Main", "net.minecraft.server.MinecraftServer", "com.mojang.minecraft.server.MinecraftServer");

	public TaskRun(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		Version currentVersion = mcp.getCurrentVersion();
		Side runSide = mcp.getOptions().side;
		if(runSide == Side.ANY) {
			runSide = side;
		}

		String main = getMain(mcp, currentVersion, side);
		if(main == null) {
			mcp.log("Start class not found");
			return;
		}

		boolean runBuild = mcp.getOptions().getBooleanParameter(TaskParameter.RUN_BUILD);
		String[] runArgs = mcp.getOptions().getStringArrayParameter(TaskParameter.RUN_ARGS);
		List<Path> cpList = getClasspath(mcp, currentVersion, runSide, runBuild);

		List<String> classPath = new ArrayList<>();
		cpList.forEach(p -> classPath.add(p.toAbsolutePath().toString()));

		Path natives = MCPPaths.get(mcp, NATIVES).toAbsolutePath();

		List<String> args = new ArrayList<>();
		args.add(Util.getJava());
		for (String arg : runArgs) {
			args.add(arg);
		}
		args.add("-Djava.library.path=" + natives);
		args.add("-cp");
		args.add(String.join(System.getProperty("path.separator"), classPath));
		args.add(main);
		if(side == Side.CLIENT) {
			for (String arg : getLaunchArgs(mcp)) {
				args.add(arg);
			}
		}

		Util.runCommand(args.toArray(new String[0]), MCPPaths.get(mcp, JARS), true);
	}

	public static String getMain(MCP mcp, Version version, Side side) throws IOException {
		if(side == Side.CLIENT) {
			return version.mainClass;
		}
		if(side == Side.SERVER) {
			Path jarPath = MCPPaths.get(mcp, JAR_ORIGINAL, side);
			try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(jarPath))) {
				ZipEntry zipEntry;
				while ((zipEntry = zipIn.getNextEntry()) != null) {
					if (zipEntry.getName().endsWith(".class")) {
						String className = zipEntry.getName().substring(0, zipEntry.getName().length() - 6).replace('\\', '.').replace('/', '.');
						if(SERVER_MAIN.contains(className)) {
							return className;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param mcp
	 * @return arguments for launching client
	 * @throws IOException
	 */
	public static List<String> getLaunchArgs(MCP mcp) throws IOException {
		Version ver = mcp.getCurrentVersion();
		Arguments args = ver.arguments;
		String mcArgs = ver.minecraftArguments;
		List<String> argsList = new ArrayList<>();
		if(args != null) {
			for(Object o : args.game) {
				if(o instanceof String) {
					argsList.add((String)o);
				}
			}
		} else {
			argsList.addAll(Arrays.asList(mcArgs.split(" ")));
		}

		Path gameDir = getMCDir(mcp, Side.CLIENT).toAbsolutePath();
		Path assets = gameDir.resolve("assets");

		for(int i = 0; i < argsList.size(); i++) {
			String arg = argsList.get(i);
			switch (arg) {

			case "${auth_player_name}": arg = "Player"; break;
			case "${auth_session}": arg = "-"; break;
			case "${user_properties}": arg = "{}"; break;
			case "${version_name}": arg = ver.id; break;
			case "${version_type}": arg = ver.type; break;
			case "${user_type}": arg = "legacy"; break;
			case "${auth_uuid}": arg = "-"; break;
			case "${auth_access_token}": arg = "-"; break;
			case "${assets_index_name}": arg = ver.assets; break;
			case "${assets_root}": arg = assets.toString(); break;
			case "${game_assets}": arg = assets.toString(); break;
			case "${game_directory}": arg = gameDir.toString(); break;
			}
			argsList.set(i, arg);
		}
		return argsList;
	}


	public static Path getMCDir(MCP mcp, Side side) throws IOException {
		Path mcDir = MCPPaths.get(mcp, GAMEDIR, side);
		if(mcDir != null) {
			return mcDir;
		}
		return getOriginalMCDir();
	}

	public static Path getOriginalMCDir() {
		String appDir = "minecraft";
		String userhome = System.getProperty("user.home", ".");
		Path path;
		switch(OS.getOs()) {
		case linux:
			path = Paths.get(userhome, '.' + appDir + '/');
			break;
		case windows:
			String appdata = System.getenv("APPDATA");
			if(appdata != null) {
				path = Paths.get(appdata, "." + appDir + '/');
			} else {
				path = Paths.get(userhome, '.' + appDir + '/');
			}
			break;
		case osx:
			path = Paths.get(userhome, "Library/Application Support/" + appDir);
			break;
		default:
			path = Paths.get(userhome, appDir + '/');
		}

		if(!Files.exists(path) && !path.toFile().mkdirs()) {
			throw new RuntimeException("The working directory could not be created: " + path);
		} else {
			return path;
		}
	}

	private static List<Path> getClasspath(MCP mcp, Version version, Side side, boolean runBuild) throws IOException {
		List<Path> cpList = new ArrayList<>();
		if(runBuild) {
			cpList.add(MCPPaths.get(mcp, BUILD_ZIP, side));
		}
		else {
			cpList.add(MCPPaths.get(mcp, BIN, side));
		}
		if(Files.exists(MCPPaths.get(mcp, REMAPPED, side))) {
			cpList.add(MCPPaths.get(mcp, REMAPPED, side));
		}
		else {
			cpList.add(MCPPaths.get(mcp, JAR_ORIGINAL, side));
		}
		if(side == Side.CLIENT || side == Side.MERGED) {
			cpList.addAll(DownloadData.getLibraries(mcp, version));
		}
		return cpList;
	}
}
