package org.mcphackers.mcp.tasks;

import static org.mcphackers.mcp.tools.FileUtil.collectJars;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.TaskParameter;
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
			throw new Exception("Server isn't available for this version!");
		}
		Path natives = MCPPaths.get(mcp, MCPPaths.NATIVES);
		List<Path> cpList = new LinkedList<>();
		if(side == Side.SERVER) {
			if(runBuild) {
				cpList.add(MCPPaths.get(mcp, MCPPaths.BUILD_ZIP_SERVER));
			}
			cpList.add(MCPPaths.get(mcp, MCPPaths.SERVER_BIN));
			cpList.add(MCPPaths.get(mcp, MCPPaths.SERVER));
			collectJars(MCPPaths.get(mcp, MCPPaths.LIB_SERVER), cpList);
		}
		else if (side == Side.CLIENT) {
			if(runBuild) {
				cpList.add(MCPPaths.get(mcp, MCPPaths.BUILD_ZIP_CLIENT));
			}
			cpList.add(MCPPaths.get(mcp, MCPPaths.CLIENT_BIN));
			if(!Files.exists(MCPPaths.get(mcp, MCPPaths.CLIENT_FIXED))) {
				cpList.add(MCPPaths.get(mcp, MCPPaths.CLIENT));
			}
			collectJars(MCPPaths.get(mcp, MCPPaths.LIB_CLIENT), cpList);
		}
		collectJars(MCPPaths.get(mcp, MCPPaths.LIB), cpList);
		
		List<String> classPath = new ArrayList<>();
		cpList.forEach(p -> classPath.add(p.toAbsolutePath().toString()));
		String cp = String.join(System.getProperty("path.separator"), classPath);

		String java = Util.getJava();
		List<String> args = new ArrayList<>(
				Arrays.asList(java,
						// TODO Would also be good if proxy could be customizable in run args
						"-Dhttp.proxyHost=betacraft.uk",
						"-Dhttp.proxyPort=" + VersionsParser.getProxyPort(currentVersion),
						"-Dorg.lwjgl.librarypath=" + natives.toAbsolutePath(),
						"-Dnet.java.games.input.librarypath=" + natives.toAbsolutePath(),
						"-cp", cp,
						side == Side.SERVER ? (VersionsParser.getServerVersion(currentVersion).startsWith("c") ? "com.mojang.minecraft.server.MinecraftServer" : "net.minecraft.server.MinecraftServer")
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
