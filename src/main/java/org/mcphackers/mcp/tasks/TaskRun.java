package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.tools.FileUtil.collectJars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.MCPPaths;
import org.mcphackers.mcp.tools.Util;
import org.mcphackers.mcp.tools.VersionsParser;

public class TaskRun extends Task {
	public TaskRun(Side side, MCP instance) {
		super(side, instance);
	}

	@Override
	public void doTask() throws Exception {
		boolean runBuild = mcp.getOptions().getBooleanParameter(TaskParameter.RUN_BUILD);
		String currentVersion = mcp.getCurrentVersion();
		if(side == Side.SERVER && !VersionsParser.hasServer(currentVersion)) {
			throw new Exception(Side.SERVER.name + " isn't available for this version!");
		}
		String host = "localhost";
		int port = 11700;
		try {
			port = VersionsParser.getProxyPort(currentVersion);
			host = "betacraft.uk";
		} catch (Exception e) {};
		
		Path natives = MCPPaths.get(mcp, MCPPaths.NATIVES);
		List<Path> cpList = new LinkedList<>();
		if(runBuild) {
			cpList.add(MCPPaths.get(mcp, MCPPaths.BUILD_ZIP, Side.MERGED));
		}
		cpList.add(MCPPaths.get(mcp, MCPPaths.BIN_SIDE, Side.MERGED));
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
		
		List<String> classPath = new ArrayList<>();
		cpList.forEach(p -> classPath.add(p.toAbsolutePath().toString()));
		String cp = String.join(System.getProperty("path.separator"), classPath);

		String java = Util.getJava();
		List<String> args = new ArrayList<>(
				Arrays.asList(java,
						"-Dhttp.proxyHost=" + host,
						"-Dhttp.proxyPort=" + port,
						"-Dorg.lwjgl.librarypath=" + natives.toAbsolutePath(),
						"-Dnet.java.games.input.librarypath=" + natives.toAbsolutePath(),
						"-cp", cp,
						findStartClass(cpList)
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
	
	private String findStartClass(List<Path> classPath) throws Exception {
		List<String> possibleStartClass = new ArrayList<>();
		possibleStartClass.add("Start");
		if(side != Side.SERVER) {
			possibleStartClass.add("net.minecraft.client.Minecraft");
			possibleStartClass.add("com.mojang.minecraft.Minecraft");
		}
		if(side != Side.CLIENT) {
			possibleStartClass.add("net.minecraft.server.MinecraftServer");
			possibleStartClass.add("com.mojang.minecraft.server.MinecraftServer");
		}
		for(Path cp : classPath) {
			for(String start : possibleStartClass) {
				if(Files.exists(cp.resolve(start))) {
					return start;
				}
			}
		}
		throw new Exception("Could not find start class");
	}
}
