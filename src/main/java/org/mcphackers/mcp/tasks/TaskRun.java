package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.tools.FileUtil.collectJars;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskRun extends Task {
	
	public static final String[] CLIENT_START = {
		"Start",
		"net.minecraft.client.Minecraft",
		"net.minecraft.client.main.Main",
		"com.mojang.minecraft.Minecraft"
	};
	
	public static final String[] SERVER_START = {
		"Start",
		"net.minecraft.server.MinecraftServer",
		"com.mojang.minecraft.server.MinecraftServer"
	};
	
	public TaskRun(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		String currentVersion = mcp.getCurrentVersion();
		String host = "localhost";
		int port = 11700;
		try {
			port = VersionsParser.getProxyPort(currentVersion);
			host = "betacraft.uk";
		} catch (Exception e) {};
		
		boolean runBuild = mcp.getOptions().getBooleanParameter(TaskParameter.RUN_BUILD);
		List<Path> cpList = getClasspath(mcp, side, runBuild);
		
		List<String> classPath = new ArrayList<>();
		cpList.forEach(p -> classPath.add(p.toAbsolutePath().toString()));
		String cp = String.join(System.getProperty("path.separator"), classPath);

		Path natives = MCPPaths.get(mcp, MCPPaths.NATIVES).toAbsolutePath();

		List<String> args = new ArrayList<>(
				Arrays.asList(
						Util.getJava(),
						"-Dhttp.proxyHost=" + host,
						"-Dhttp.proxyPort=" + port,
						"-Dorg.lwjgl.librarypath=" + natives,
						"-Dnet.java.games.input.librarypath=" + natives,
						"-cp", cp,
						findStartClass(cpList, side)
						)
				);
		String[] runArgs = mcp.getOptions().getStringArrayParameter(TaskParameter.RUN_ARGS);
		for (String arg : runArgs) {
			args.add(1, arg);
		}
		int exit = Util.runCommand(args.toArray(new String[0]), MCPPaths.get(mcp, MCPPaths.JARS), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}
	
	public static String findStartClass(MCP mcp, Side side) throws Exception {
		return findStartClass(getClasspath(mcp, side, false), side);
	}

	private static List<Path> getClasspath(MCP mcp, Side side, boolean runBuild) throws IOException {
		List cpList = new ArrayList<>();
		if(runBuild) {
			cpList.add(MCPPaths.get(mcp, MCPPaths.BUILD_ZIP, side));
		}
		cpList.add(MCPPaths.get(mcp, MCPPaths.BIN_SIDE, side));
		if(Files.exists(MCPPaths.get(mcp, MCPPaths.BIN_SIDE, Side.MERGED))) {
			cpList.add(MCPPaths.get(mcp, MCPPaths.BIN_SIDE, Side.MERGED));
		}
		if(side == Side.SERVER) {
			cpList.add(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
		}
		else if (side == Side.CLIENT) {
			if(!Files.exists(MCPPaths.get(mcp, MCPPaths.CLIENT_FIXED))) {
				cpList.add(MCPPaths.get(mcp, MCPPaths.JAR_ORIGINAL, side));
			}
		}
		collectJars(MCPPaths.get(mcp, MCPPaths.LIBS, side), cpList);
		collectJars(MCPPaths.get(mcp, MCPPaths.LIB), cpList);
		return cpList;
	}

	private static String findStartClass(List<Path> classPath, Side side) throws Exception {
		String[] possibleStartClass;
		if(side == Side.CLIENT) {
			possibleStartClass = CLIENT_START;
		}
		else if(side == Side.SERVER) {
			possibleStartClass = SERVER_START;
		}
		else {
			possibleStartClass = new String[] {"Start"};
		}
		for(Path cp : classPath) {
			if(!Files.exists(cp)) {
				continue;
			}
			if(Files.isDirectory(cp)) {
				for(String start : possibleStartClass) {
					if(Files.exists(cp.resolve(start.replace(".", "/") + ".class"))) {
						return start;
					}
				}
			}
//			else {
//				try (FileSystem fs = FileSystems.newFileSystem(cp, (ClassLoader)null)) {
//					for(String start : possibleStartClass) {
//						if(Files.exists(fs.getPath("/").resolve(start.replace(".", "/") + ".class"))) {
//							return start;
//						}
//					}
//				}
//			}
		}
		throw new Exception("Could not find start class");
	}
}
