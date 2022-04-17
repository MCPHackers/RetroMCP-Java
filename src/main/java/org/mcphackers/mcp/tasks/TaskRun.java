package org.mcphackers.mcp.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tools.FileUtil;
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
		String java = Util.getJava();
		String natives = FileUtil.absolutePathString(MCPPaths.NATIVES);
		List<String> cpList = new LinkedList<>();
		if(side == Side.SERVER) {
			if(runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPPaths.BUILD_ZIP_SERVER));
			}
			cpList.add(FileUtil.absolutePathString(MCPPaths.SERVER_BIN));
			cpList.add(FileUtil.absolutePathString(MCPPaths.SERVER));
			try(Stream<Path> stream = Files.list(MCPPaths.get(mcp, MCPPaths.LIB_SERVER)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				cpList.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
			}
		}
		else if (side == Side.CLIENT) {
			if(runBuild) {
				cpList.add(FileUtil.absolutePathString(MCPPaths.BUILD_ZIP_CLIENT));
			}
			cpList.add(FileUtil.absolutePathString(MCPPaths.CLIENT_BIN));
			if(!Files.exists(MCPPaths.get(mcp, MCPPaths.CLIENT_FIXED))) {
				cpList.add(FileUtil.absolutePathString(MCPPaths.CLIENT));
			}
			try(Stream<Path> stream = Files.list(MCPPaths.get(mcp, MCPPaths.LIB)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				cpList.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
			}
			try(Stream<Path> stream = Files.list(MCPPaths.get(mcp, MCPPaths.LIB_CLIENT)).filter(library -> !library.endsWith(".jar")).filter(library -> !Files.isDirectory(library))) {
				cpList.addAll(stream.map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.toList()));
			}
		}
		
		
		String cp = String.join(System.getProperty("path.separator"), cpList);

		List<String> args = new ArrayList<>(
				Arrays.asList(java,
						// TODO Would also be good if proxy could be customizable in run args
						"-Dhttp.proxyHost=betacraft.uk",
						"-Dhttp.proxyPort=" + VersionsParser.getProxyPort(currentVersion),
						"-Dorg.lwjgl.librarypath=" + natives,
						"-Dnet.java.games.input.librarypath=" + natives,
						"-cp", cp,
						side == Side.SERVER ? (VersionsParser.getServerVersion(currentVersion).startsWith("c") ? "com.mojang.minecraft.server.MinecraftServer" : "net.minecraft.server.MinecraftServer")
																	  : runBuild ? "net.minecraft.client.Minecraft" : "Start"));
		String[] runArgs = mcp.getOptions().getStringArrayParameter(TaskParameter.RUN_ARGS);
		for(int i = 0; i < runArgs.length; i++) {
			String arg = runArgs[i];
			args.add(1, arg);
		}
		int exit = Util.runCommand(args.toArray(new String[0]), MCPPaths.get(mcp, MCPPaths.JARS), true);
		if(exit != 0) {
			throw new RuntimeException("Finished with exit value " + exit);
		}
	}
}
