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
		int port = VersionsParser.getProxyPort(currentVersion);
		boolean isClassic = VersionsParser.getServerVersion(currentVersion).startsWith("c");
		Path natives = MCPPaths.get(mcp, MCPPaths.NATIVES);
		List<Path> cpList = new LinkedList<>();
		if(runBuild) {
			cpList.add(MCPPaths.get(mcp, MCPPaths.BUILD_ZIP, side));
		}
		cpList.add(MCPPaths.get(mcp, MCPPaths.BIN_SIDE, side));
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
						// TODO Would also be good if proxy could be customizable in run args
						"-Dhttp.proxyHost=betacraft.uk",
						"-Dhttp.proxyPort=" + port,
						"-Dorg.lwjgl.librarypath=" + natives.toAbsolutePath(),
						"-Dnet.java.games.input.librarypath=" + natives.toAbsolutePath(),
						"-cp", cp,
						side == Side.SERVER ? (isClassic ? "com.mojang.minecraft.server.MinecraftServer" : "net.minecraft.server.MinecraftServer")
																	  : runBuild ? "net.minecraft.client.Minecraft" : "Start"));
		String[] runArgs = mcp.getOptions().getStringArrayParameter(TaskParameter.RUN_ARGS);
		for (String arg : runArgs) {
			args.add(1, arg);
		}
		int exit = Util.runCommand(args.toArray(new String[0]), MCPPaths.get(mcp, MCPPaths.JARS), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}
}
