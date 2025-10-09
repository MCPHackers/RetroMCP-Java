package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.MCPPaths.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.versions.json.Rule;
import org.mcphackers.mcp.tools.versions.json.Version;
import org.mcphackers.mcp.tools.versions.json.Version.Argument;
import org.mcphackers.mcp.tools.versions.json.Version.Arguments;

public class TaskRun extends TaskStaged {

	public static final List<String> SERVER_MAIN = Arrays.asList("net.minecraft.server.Main", "net.minecraft.server.MinecraftServer", "com.mojang.minecraft.server.MinecraftServer");

	public TaskRun(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	protected Stage[] setStages() {
		return new Stage[] {
				stage(getLocalizedStage("run"), () -> {
					Version currentVersion = mcp.getCurrentVersion();
					Side mcpSide = mcp.getOptions().side;
					if (mcpSide == Side.ANY) {
						mcpSide = side;
					}

					String main = getMain(mcp, currentVersion, side);
					if (main == null) {
						mcp.log("Start class not found");
						return;
					}
					mcp.log("Using main class: " + main);

					boolean runBuild = mcp.getOptions().getBooleanParameter(TaskParameter.RUN_BUILD);
					boolean fullBuild = mcp.getOptions().getBooleanParameter(TaskParameter.FULL_BUILD);
					String[] runArgs = mcp.getOptions().getStringArrayParameter(TaskParameter.RUN_ARGS);
					List<Path> cpList = getClasspath(mcp, mcpSide, side, runBuild, fullBuild);

					List<String> classPath = new ArrayList<>();
					cpList.forEach(p -> classPath.add(p.toAbsolutePath().toString()));

					Path natives = MCPPaths.get(mcp, NATIVES).toAbsolutePath();

					List<String> args = new ArrayList<>();
					List<String> gameArgs = new ArrayList<>();
					args.add(Util.getJava());
					String cpString = String.join(File.pathSeparator, classPath);
					for(String s : getJvmArgs(mcp, mcpSide)) {
						args.add(s.replace("${classpath}", cpString)
						.replace("${natives_directory}", natives.toAbsolutePath().toString())
						.replace("${launcher_name}", "RetroMCP")
						.replace("${launcher_version}", MCP.VERSION));
					}
					Collections.addAll(args, runArgs);
					args.add(main);
					if (side == Side.CLIENT) {
						gameArgs.addAll(getLaunchArgs(mcp, mcpSide));
						Collections.addAll(gameArgs, mcp.getOptions().getStringParameter(TaskParameter.GAME_ARGS).split(" "));
						args.addAll(gameArgs);
					}
					mcp.log("Launch arguments: " + String.join(", ", args));
					// mcp.log("Classpath:\n" + String.join("\n", classPath));

					Util.runCommand(args.toArray(new String[0]), getMCDir(mcp, mcpSide), true);
				})
		};
	}

	public static String getMain(MCP mcp, Version version, Side side) throws IOException {
		if (side == Side.CLIENT) {
			return version.mainClass;
		}
		if (side == Side.SERVER) {
			Path jarPath = MCPPaths.get(mcp, JAR_ORIGINAL, Side.SERVER);
			try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(jarPath))) {
				ZipEntry zipEntry;
				while ((zipEntry = zipIn.getNextEntry()) != null) {
					if (zipEntry.getName().endsWith(".class")) {
						String className = zipEntry.getName().substring(0, zipEntry.getName().length() - 6).replace('\\', '.').replace('/', '.');
						if (SERVER_MAIN.contains(className)) {
							return className;
						}
					}
				}
			}
		}
		return null;
	}

	private static List<String> getStringArguments(List<Object> objects) {
		List<String> argsList = new ArrayList<>();
		for (Object o : objects) {
			if (o instanceof String) {
				argsList.add((String) o);
			}
			else if (o instanceof Argument) {
				Argument arg = (Argument)o;
				if(Rule.apply(arg.rules)) {
					if(arg.value instanceof String) {
						argsList.add((String)arg.value);
					}
					else if(arg.value instanceof JSONArray) {
						JSONArray arr = (JSONArray)arg.value;
						for(int i = 0; i < arr.length(); i++) {
							argsList.add(arr.getString(i));
						}
					}
				}
			}
		}
		return argsList;
	}

	public static List<String> getJvmArgs(MCP mcp, Side side) {
		List<String> argsList = new ArrayList<>();
		Version ver = mcp.getCurrentVersion();
		if(ver.arguments != null) {
			argsList.addAll(getStringArguments(ver.arguments.jvm));
		} else {
			argsList.add("-Djava.library.path=${natives_directory}");
			argsList.add("-cp");
			argsList.add("${classpath}");
		}
		return argsList;
	}

	/**
	 * @param mcp
	 * @return arguments for launching client
	 */
	public static List<String> getLaunchArgs(MCP mcp, Side side) {
		Version ver = mcp.getCurrentVersion();
		Arguments args = ver.arguments;
		String mcArgs = ver.minecraftArguments;
		List<String> argsList = new ArrayList<>();
		if (args != null) {
			argsList.addAll(getStringArguments(args.game));
		} else {
			argsList.addAll(Arrays.asList(mcArgs.split(" ")));
		}

		Path gameDir = getMCDir(mcp, side);
		Path assets = gameDir.resolve("assets");

		for (int i = 0; i < argsList.size(); i++) {
			String arg = argsList.get(i);
			switch (arg) {
				case "${auth_player_name}":
					arg = "Player";
					break;
				case "${auth_session}":
				case "${auth_uuid}":
				case "${auth_access_token}":
					arg = "-";
					break;
				case "${user_properties}":
					arg = "{}";
					break;
				case "${version_name}":
					arg = ver.id;
					break;
				case "${version_type}":
					arg = ver.type;
					break;
				case "${user_type}":
					arg = "legacy";
					break;
				case "${assets_index_name}":
					arg = ver.assets;
					break;
				case "${assets_root}":
				case "${game_assets}":
					arg = assets.toString();
					break;
				case "${game_directory}":
					arg = gameDir.toString();
					break;
			}
			argsList.set(i, arg);
		}
		return argsList;
	}

	public static Path getMCDir(MCP mcp, Side side) {
		return MCPPaths.get(mcp, MCPPaths.PROJECT, side).relativize(MCPPaths.get(mcp, GAMEDIR, side));
	}

	private static List<Path> getClasspath(MCP mcp, Side side, Side runSide, boolean runBuild, boolean fullBuild) {
		List<Path> cpList = new ArrayList<>(mcp.getLibraries());
		if (runBuild) {
			if (fullBuild) {
				cpList.add(MCPPaths.get(mcp, BUILD_JAR, runSide));
			} else {
				cpList.add(MCPPaths.get(mcp, BUILD_ZIP, runSide));
				cpList.add(MCPPaths.get(mcp, JAR_ORIGINAL, runSide));
			}
		} else {
			cpList.add(MCPPaths.get(mcp, BIN, side));
			if (Files.exists(MCPPaths.get(mcp, REMAPPED, side))) {
				cpList.add(MCPPaths.get(mcp, REMAPPED, side));
			}
		}
		return cpList;
	}
}
